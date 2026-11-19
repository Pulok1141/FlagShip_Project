package io.flagship.starter;

import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.model.EnvironmentSnapshot;
import io.flagship.core.model.FlagDefinition;
import io.flagship.core.model.FlagType;
import io.flagship.core.model.SnapshotJsonMapper;
import io.flagship.starter.config.FlagShipProperties;
import io.flagship.starter.sync.SnapshotCache;
import io.flagship.starter.sync.SyncEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StartupTimeoutAndDiskCacheTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("FR6 / QA2: Unreachable server drops to local disk snapshot cache within startup timeout budget")
    void testStartupResilienceWithDiskCache() {
        Path cacheFile = tempDir.resolve("test-snapshot.json");
        SnapshotCache cache = new SnapshotCache(cacheFile.toString());

        // Pre-seed disk cache with valid snapshot
        FlagDefinition flag1 = new FlagDefinition("cached-flag-1", "Flag 1", "Desc", FlagType.BOOLEAN,
                true, List.of(), List.of(), List.of(), null, "default", "off", 10L, Instant.now(), List.of());
        FlagDefinition flag2 = new FlagDefinition("cached-flag-2", "Flag 2", "Desc", FlagType.STRING,
                true, List.of(), List.of(), List.of(), null, "default", "off", 10L, Instant.now(), List.of());

        EnvironmentSnapshot initialSnapshot = new EnvironmentSnapshot("p1", "development", 10L,
                Instant.now(), java.util.Map.of("cached-flag-1", flag1, "cached-flag-2", flag2), java.util.Map.of());
        cache.save(SnapshotJsonMapper.toJson(initialSnapshot));

        // Configure properties pointing to an unreachable port with 400ms timeout
        FlagShipProperties props = new FlagShipProperties();
        props.setServerUrl("http://127.0.0.1:59999");
        props.setStartupTimeoutMs(400);
        props.setCacheFilePath(cacheFile.toString());
        props.setApiKey("test-key");

        SyncEngine syncEngine = new SyncEngine(props, cache);

        long startTime = System.currentTimeMillis();
        CompiledSnapshot snapshot = syncEngine.bootstrap();
        long duration = System.currentTimeMillis() - startTime;

        // Verify startup completed within budget
        assertThat(duration).isLessThan(2000L); // Well within QA2 <= 2000 ms cap

        // Verify snapshot was loaded from disk cache
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.version()).isEqualTo(10L);
        assertThat(snapshot.flags()).hasSize(2);
        assertThat(snapshot.flags().keySet()).containsExactlyInAnyOrder("cached-flag-1", "cached-flag-2");
        assertThat(syncEngine.getSyncMode()).isEqualTo("DEGRADED_DISK_CACHE");

        syncEngine.close();
    }
}
