package io.flagship.starter.sync;

import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.model.EnvironmentSnapshot;
import io.flagship.core.model.SnapshotJsonMapper;
import io.flagship.starter.client.FlagShipClient;
import io.flagship.starter.config.FlagShipProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Real-time synchronization engine delivering updates via SSE push with polling fallback (FR5, FR6, QA2, QA3).
 * Uses lightweight, robust HttpURLConnection to avoid platform-specific loopback selector socket blocks.
 */
public class SyncEngine implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(SyncEngine.class);

    private final FlagShipProperties props;
    private final SnapshotCache cache;
    private final ScheduledExecutorService pollingExecutor;
    private final AtomicBoolean running = new AtomicBoolean(true);

    private FlagShipClient client;
    private final AtomicReference<String> syncMode = new AtomicReference<>("DISCONNECTED");
    private final AtomicReference<Instant> lastSyncTime = new AtomicReference<>(Instant.now());
    private final AtomicReference<String> lastEtag = new AtomicReference<>(null);

    public SyncEngine(FlagShipProperties props, SnapshotCache cache) {
        this.props = props;
        this.cache = cache;

        this.pollingExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "flagship-polling-sync");
            t.setDaemon(true);
            return t;
        });
    }

    public void setClient(FlagShipClient client) {
        this.client = client;
    }

    /**
     * Bootstrap startup initialization strictly capped at startupTimeoutMs (<= 2000 ms).
     * If server is unreachable, immediately falls back to local disk cache (QA2).
     */
    public CompiledSnapshot bootstrap() {
        if (props.isOfflineMode()) {
            log.info("FlagShip running in OFFLINE mode, loading local disk cache snapshot");
            syncMode.set("OFFLINE");
            CompiledSnapshot cached = cache.load();
            return cached != null ? cached : CompiledSnapshot.empty();
        }

        long timeoutMs = props.getStartupTimeoutMs();
        log.info("Bootstrapping FlagShip client from {} (startup timeout: {} ms)", props.getServerUrl(), timeoutMs);

        // Attempt initial fetch within timeout budget
        CompletableFuture<CompiledSnapshot> fetchFuture = CompletableFuture.supplyAsync(this::fetchSnapshotFromServer);

        try {
            CompiledSnapshot serverSnapshot = fetchFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (serverSnapshot != null) {
                syncMode.set(props.isSseEnabled() ? "SSE" : "POLLING");
                lastSyncTime.set(Instant.now());
                log.info("FlagShip bootstrap successful from server: {} flags loaded (version {})",
                        serverSnapshot.flags().size(), serverSnapshot.version());
                return serverSnapshot;
            }
        } catch (TimeoutException te) {
            log.warn("FlagShip bootstrap timed out after {} ms! Falling back to local disk snapshot cache (QA2)", timeoutMs);
            fetchFuture.cancel(true);
        } catch (Exception ex) {
            log.warn("FlagShip bootstrap failed to reach server [{}]: {}. Falling back to disk cache (QA2)",
                    props.getServerUrl(), ex.getMessage());
        }

        // Fallback to disk snapshot cache
        CompiledSnapshot cached = cache.load();
        if (cached != null) {
            syncMode.set("DEGRADED_DISK_CACHE");
            lastSyncTime.set(Instant.now());
            log.info("FlagShip restored {} flags from local disk cache snapshot (version {})",
                    cached.flags().size(), cached.version());
            return cached;
        }

        // If no cache exists, return empty snapshot (0 exceptions guarantee)
        log.warn("No FlagShip disk snapshot cache found. Starting with empty snapshot.");
        syncMode.set("EMPTY_FALLBACK");
        return CompiledSnapshot.empty();
    }

    public void startBackgroundSync() {
        if (props.isOfflineMode() || !running.get()) return;

        if (props.isSseEnabled()) {
            Thread.ofVirtual().name("flagship-sse-listener").start(this::sseSyncLoop);
        } else {
            syncMode.set("POLLING");
            long interval = props.getPollingIntervalMs();
            pollingExecutor.scheduleWithFixedDelay(this::pollSnapshot, interval, interval, TimeUnit.MILLISECONDS);
        }
    }

    private void sseSyncLoop() {
        while (running.get()) {
            HttpURLConnection conn = null;
            try {
                String sseUrl = cleanServerUrl() + "/api/sdk/v1/stream?apiKey=" + props.getApiKey();
                log.info("Connecting to FlagShip SSE stream: {}", sseUrl);

                URI uri = URI.create(sseUrl);
                conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(0); // Long-lived stream
                conn.setRequestProperty("Accept", "text/event-stream");
                conn.setRequestProperty("X-FlagShip-Key", props.getApiKey());

                int status = conn.getResponseCode();
                if (status == 200) {
                    syncMode.set("SSE");
                    lastSyncTime.set(Instant.now());
                    log.info("FlagShip SSE stream connected successfully");

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        String currentEvent = "message";
                        StringBuilder dataBuffer = new StringBuilder();

                        while (running.get() && (line = reader.readLine()) != null) {
                            if (line.startsWith("event:")) {
                                currentEvent = line.substring(6).trim();
                            } else if (line.startsWith("data:")) {
                                dataBuffer.append(line.substring(5).trim());
                            } else if (line.isEmpty() && dataBuffer.length() > 0) {
                                handleSseEvent(currentEvent, dataBuffer.toString());
                                currentEvent = "message";
                                dataBuffer.setLength(0);
                            }
                        }
                    }
                } else {
                    log.warn("FlagShip SSE stream rejected: HTTP {}", status);
                }
            } catch (Exception ex) {
                if (running.get()) {
                    log.debug("FlagShip SSE connection dropped: {}. Falling back to polling...", ex.getMessage());
                }
            } finally {
                if (conn != null) conn.disconnect();
            }

            // Fallback polling while SSE is disconnected
            if (running.get()) {
                syncMode.set("POLLING_FALLBACK");
                pollSnapshot();
                try {
                    Thread.sleep(Math.min(props.getPollingIntervalMs(), 10000L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void handleSseEvent(String eventType, String data) {
        lastSyncTime.set(Instant.now());
        if ("heartbeat".equalsIgnoreCase(eventType)) {
            log.trace("Received FlagShip SSE heartbeat");
            return;
        }

        if ("snapshot".equalsIgnoreCase(eventType) || "flag-update".equalsIgnoreCase(eventType)
                || "flag_updated".equalsIgnoreCase(eventType) || "flag_update".equalsIgnoreCase(eventType)) {
            try {
                EnvironmentSnapshot snapshot = SnapshotJsonMapper.fromJson(data, EnvironmentSnapshot.class);
                if (snapshot != null && snapshot.flags() != null && !snapshot.flags().isEmpty()) {
                    CompiledSnapshot compiled = new CompiledSnapshot(snapshot);
                    if (client != null) {
                        client.updateSnapshot(compiled);
                    }
                    cache.save(data);
                    log.info("Applied FlagShip live update via SSE (version {})", snapshot.snapshotVersion());
                    return;
                }
            } catch (Exception ignored) {
            }
            // Fetch updated snapshot via ETag immediately upon push event
            pollSnapshot();
        }
    }

    public synchronized void pollSnapshot() {
        if (!running.get() || props.isOfflineMode()) return;

        HttpURLConnection conn = null;
        try {
            URI uri = URI.create(cleanServerUrl() + "/api/sdk/v1/snapshot");
            conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("X-FlagShip-Key", props.getApiKey());

            String etag = lastEtag.get();
            if (etag != null && !etag.isBlank()) {
                conn.setRequestProperty("If-None-Match", etag);
            }

            int code = conn.getResponseCode();
            if (code == 304) {
                lastSyncTime.set(Instant.now());
                log.debug("FlagShip snapshot unchanged (ETag 304)");
                return;
            }

            if (code == 200) {
                String newEtag = conn.getHeaderField("ETag");
                if (newEtag != null) {
                    lastEtag.set(newEtag);
                }

                try (InputStream is = conn.getInputStream()) {
                    String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    EnvironmentSnapshot snapshot = SnapshotJsonMapper.fromJson(json, EnvironmentSnapshot.class);
                    if (snapshot != null) {
                        CompiledSnapshot compiled = new CompiledSnapshot(snapshot);
                        if (client != null) {
                            client.updateSnapshot(compiled);
                        }
                        cache.save(json);
                        lastSyncTime.set(Instant.now());
                        log.debug("FlagShip snapshot updated via polling: {} flags (version {})",
                                snapshot.flags().size(), snapshot.snapshotVersion());
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("FlagShip polling error: {}", ex.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private CompiledSnapshot fetchSnapshotFromServer() {
        HttpURLConnection conn = null;
        try {
            URI uri = URI.create(cleanServerUrl() + "/api/sdk/v1/snapshot");
            conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            int timeout = (int) Math.min(props.getStartupTimeoutMs(), 2000L);
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("X-FlagShip-Key", props.getApiKey());

            int code = conn.getResponseCode();
            if (code == 200) {
                String etag = conn.getHeaderField("ETag");
                if (etag != null) lastEtag.set(etag);

                try (InputStream is = conn.getInputStream()) {
                    String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    EnvironmentSnapshot snapshot = SnapshotJsonMapper.fromJson(json, EnvironmentSnapshot.class);
                    if (snapshot != null) {
                        cache.save(json);
                        return new CompiledSnapshot(snapshot);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("fetchSnapshotFromServer failed: {}", e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private String cleanServerUrl() {
        String url = props.getServerUrl();
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public String getSyncMode() {
        return syncMode.get();
    }

    public Instant getLastSyncTime() {
        return lastSyncTime.get();
    }

    public String getLastEtag() {
        return lastEtag.get();
    }

    @Override
    public void close() {
        running.set(false);
        pollingExecutor.shutdownNow();
    }
}
