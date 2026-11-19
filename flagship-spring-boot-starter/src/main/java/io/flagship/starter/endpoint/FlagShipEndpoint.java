package io.flagship.starter.endpoint;

import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.starter.client.FlagShipClient;
import io.flagship.starter.config.FlagShipProperties;
import io.flagship.starter.sync.SyncEngine;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom Spring Boot Actuator Endpoint exposed at /actuator/flagship (FR4).
 * Surfaces operational health, loaded flag counts, cache path, and real-time sync mode.
 */
@Endpoint(id = "flagship")
public class FlagShipEndpoint {

    private final FlagShipProperties props;
    private final FlagShipClient client;
    private final SyncEngine syncEngine;

    public FlagShipEndpoint(FlagShipProperties props, FlagShipClient client, SyncEngine syncEngine) {
        this.props = props;
        this.client = client;
        this.syncEngine = syncEngine;
    }

    @ReadOperation
    public Map<String, Object> flagshipInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("enabled", props.isEnabled());
        info.put("serverUrl", props.getServerUrl());
        info.put("cacheFilePath", props.getCacheFilePath());
        info.put("syncMode", syncEngine != null ? syncEngine.getSyncMode() : "OFFLINE");
        info.put("lastSyncTime", syncEngine != null && syncEngine.getLastSyncTime() != null
                ? syncEngine.getLastSyncTime().toString() : Instant.now().toString());
        info.put("etag", syncEngine != null ? syncEngine.getLastEtag() : null);

        CompiledSnapshot snapshot = client != null ? client.getSnapshot() : null;
        if (snapshot != null) {
            info.put("snapshotVersion", snapshot.version());
            info.put("loadedFlagsCount", snapshot.flags().size());
            info.put("loadedFlagKeys", new ArrayList<>(snapshot.flags().keySet()));
        } else {
            info.put("snapshotVersion", 0L);
            info.put("loadedFlagsCount", 0);
            info.put("loadedFlagKeys", java.util.List.of());
        }

        return info;
    }
}
