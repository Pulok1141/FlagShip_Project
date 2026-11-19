package io.flagship.starter.sync;

import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.model.EnvironmentSnapshot;
import io.flagship.core.model.SnapshotJsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Atomic local disk snapshot persistence cache for offline resilience (FR6, QA2).
 * Writes via a temporary file before atomically renaming to prevent partial writes.
 */
public class SnapshotCache {

    private static final Logger log = LoggerFactory.getLogger(SnapshotCache.class);

    private final Path cacheFilePath;

    public SnapshotCache(String pathStr) {
        this.cacheFilePath = Paths.get(pathStr != null ? pathStr : "./.flagship/snapshot.json");
    }

    public synchronized void save(String snapshotJson) {
        if (snapshotJson == null || snapshotJson.isBlank()) return;

        try {
            Path parent = cacheFilePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            Path tmpFile = Paths.get(cacheFilePath.toString() + ".tmp");
            Files.writeString(tmpFile, snapshotJson, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

            try {
                Files.move(tmpFile, cacheFilePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmpFile, cacheFilePath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.debug("Successfully persisted FlagShip snapshot to disk cache: {}", cacheFilePath);
        } catch (Exception e) {
            log.warn("Failed to persist FlagShip snapshot to local disk cache [{}]: {}", cacheFilePath, e.getMessage());
        }
    }

    public synchronized CompiledSnapshot load() {
        if (!Files.exists(cacheFilePath)) {
            log.debug("No FlagShip local snapshot cache found at {}", cacheFilePath);
            return null;
        }

        try {
            String json = Files.readString(cacheFilePath, StandardCharsets.UTF_8);
            EnvironmentSnapshot snapshot = SnapshotJsonMapper.fromJson(json, EnvironmentSnapshot.class);
            if (snapshot != null) {
                log.info("Loaded FlagShip snapshot from local disk cache: {} flags, version {}",
                        snapshot.flags() != null ? snapshot.flags().size() : 0, snapshot.snapshotVersion());
                return new CompiledSnapshot(snapshot);
            }
        } catch (Exception e) {
            log.warn("Failed to load FlagShip snapshot from disk cache [{}]: {}", cacheFilePath, e.getMessage());
        }
        return null;
    }

    public Path getCacheFilePath() {
        return cacheFilePath;
    }
}
