package io.flagship.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flagship")
public class FlagShipProperties {

    /**
     * Master switch for FlagShip SDK integration.
     */
    private boolean enabled = true;

    /**
     * FlagShip Control Plane Server URL (e.g. http://localhost:8080).
     */
    private String serverUrl = "http://localhost:8080";

    /**
     * Project environment SDK API key (e.g. flagship_dev_key_12345).
     */
    private String apiKey = "";

    /**
     * Startup bootstrap blocking timeout in milliseconds. Strictly capped at <= 2000 ms.
     */
    private long startupTimeoutMs = 1500L;

    /**
     * Local disk cache file path for atomic snapshot persistence and offline recovery.
     */
    private String cacheFilePath = "./.flagship/snapshot.json";

    /**
     * Enable real-time push synchronization via Server-Sent Events (SSE).
     */
    private boolean sseEnabled = true;

    /**
     * Polling fallback interval in milliseconds when SSE is unavailable or disconnected. Minimum 5000ms.
     */
    private long pollingIntervalMs = 30000L;

    /**
     * Asynchronous exposure & goal event batch flush interval in milliseconds.
     */
    private long eventFlushIntervalMs = 10000L;

    /**
     * Maximum number of events to batch before triggering an immediate flush.
     */
    private int eventBatchSize = 100;

    /**
     * If true, runs entirely offline using local disk cache without contacting server.
     */
    private boolean offlineMode = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public long getStartupTimeoutMs() {
        return Math.min(startupTimeoutMs, 2000L); // Strictly capped at <= 2000 ms
    }

    public void setStartupTimeoutMs(long startupTimeoutMs) {
        this.startupTimeoutMs = Math.min(startupTimeoutMs, 2000L); // Cap at 2000ms
    }

    public String getCacheFilePath() {
        return cacheFilePath;
    }

    public void setCacheFilePath(String cacheFilePath) {
        this.cacheFilePath = cacheFilePath;
    }

    public boolean isSseEnabled() {
        return sseEnabled;
    }

    public void setSseEnabled(boolean sseEnabled) {
        this.sseEnabled = sseEnabled;
    }

    public long getPollingIntervalMs() {
        return Math.max(pollingIntervalMs, 5000L); // Minimum 5s
    }

    public void setPollingIntervalMs(long pollingIntervalMs) {
        this.pollingIntervalMs = Math.max(pollingIntervalMs, 5000L);
    }

    public long getEventFlushIntervalMs() {
        return eventFlushIntervalMs;
    }

    public void setEventFlushIntervalMs(long eventFlushIntervalMs) {
        this.eventFlushIntervalMs = eventFlushIntervalMs;
    }

    public int getEventBatchSize() {
        return eventBatchSize;
    }

    public void setEventBatchSize(int eventBatchSize) {
        this.eventBatchSize = eventBatchSize;
    }

    public boolean isOfflineMode() {
        return offlineMode;
    }

    public void setOfflineMode(boolean offlineMode) {
        this.offlineMode = offlineMode;
    }
}
