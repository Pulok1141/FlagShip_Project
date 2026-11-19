package io.flagship.starter.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.flagship.core.model.SnapshotJsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * High-throughput asynchronous event batcher for EXPOSURE and GOAL events (FR8).
 * Uses a non-blocking bounded queue to prevent blocking application hot-paths.
 */
public class AsyncEventBatcher implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(AsyncEventBatcher.class);

    public record QueuedEvent(
            String flagKey,
            String variationId,
            String userKey,
            String eventType, // EXPOSURE, GOAL
            String goalName,
            double value,
            Instant timestamp
    ) {}

    private final String serverUrl;
    private final String apiKey;
    private final int batchSize;
    private final BlockingQueue<QueuedEvent> queue;
    private final ScheduledExecutorService scheduler;
    private final ObjectMapper mapper = SnapshotJsonMapper.getMapper();
    private final AtomicBoolean running = new AtomicBoolean(true);

    public AsyncEventBatcher(String serverUrl, String apiKey, long flushIntervalMs, int batchSize, int queueCapacity) {
        this.serverUrl = serverUrl != null && serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        this.apiKey = apiKey;
        this.batchSize = batchSize;
        this.queue = new ArrayBlockingQueue<>(queueCapacity > 0 ? queueCapacity : 10000);

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "flagship-event-batcher");
            t.setDaemon(true);
            return t;
        });

        long interval = Math.max(flushIntervalMs, 1000L);
        this.scheduler.scheduleAtFixedRate(this::flush, interval, interval, TimeUnit.MILLISECONDS);
    }

    public void recordExposure(String flagKey, String variationId, String userKey) {
        if (!running.get() || apiKey == null || apiKey.isBlank()) return;
        QueuedEvent event = new QueuedEvent(flagKey, variationId, userKey, "EXPOSURE", null, 1.0, Instant.now());
        if (!queue.offer(event)) {
            log.warn("FlagShip event queue full, dropping exposure event for flag: {}", flagKey);
        }
    }

    public void recordGoal(String flagKey, String userKey, String goalName, double value) {
        if (!running.get() || apiKey == null || apiKey.isBlank()) return;
        QueuedEvent event = new QueuedEvent(flagKey, null, userKey, "GOAL", goalName, value, Instant.now());
        if (!queue.offer(event)) {
            log.warn("FlagShip event queue full, dropping goal event for flag: {}", flagKey);
        }
    }

    public synchronized void flush() {
        if (queue.isEmpty() || apiKey == null || apiKey.isBlank()) return;

        List<QueuedEvent> batch = new ArrayList<>(batchSize);
        queue.drainTo(batch, batchSize);

        if (batch.isEmpty()) return;

        try {
            List<Map<String, Object>> payloadEvents = new ArrayList<>(batch.size());
            for (QueuedEvent e : batch) {
                payloadEvents.add(Map.of(
                        "flagKey", e.flagKey() != null ? e.flagKey() : "",
                        "variationId", e.variationId() != null ? e.variationId() : "",
                        "userKey", e.userKey() != null ? e.userKey() : "anonymous",
                        "eventType", e.eventType(),
                        "goalName", e.goalName() != null ? e.goalName() : "",
                        "value", e.value(),
                        "timestamp", e.timestamp().toString()
                ));
            }

            String json = mapper.writeValueAsString(Map.of("events", payloadEvents));

            URI uri = URI.create(serverUrl + "/api/sdk/v1/events");
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-FlagShip-Key", apiKey);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code >= 400) {
                log.warn("Failed to flush FlagShip events to server: HTTP {}", code);
            } else {
                log.debug("Successfully flushed {} FlagShip events", batch.size());
            }
            conn.disconnect();
        } catch (Exception ex) {
            log.debug("Error flushing FlagShip events: {}", ex.getMessage());
        }
    }

    @Override
    public void close() {
        running.set(false);
        try {
            flush();
            scheduler.shutdown();
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (Exception ignored) {}
    }
}
