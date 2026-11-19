package io.flagship.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable snapshot of all flags and segments for a specific project and environment.
 */
public record EnvironmentSnapshot(
        @JsonProperty("projectId") String projectId,
        @JsonProperty("environmentKey") String environmentKey,
        @JsonProperty("version") long version,
        @JsonProperty("updatedAt") Instant updatedAt,
        @JsonProperty("flags") Map<String, FlagDefinition> flags,
        @JsonProperty("segments") Map<String, SegmentDefinition> segments
) {
    @JsonCreator
    public EnvironmentSnapshot {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(environmentKey, "environmentKey must not be null");
        flags = flags != null ? Collections.unmodifiableMap(flags) : Collections.emptyMap();
        segments = segments != null ? Collections.unmodifiableMap(segments) : Collections.emptyMap();
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    public static EnvironmentSnapshot empty(String projectId, String environmentKey) {
        return new EnvironmentSnapshot(
                projectId,
                environmentKey,
                0L,
                Instant.EPOCH,
                Collections.emptyMap(),
                Collections.emptyMap()
        );
    }

    public FlagDefinition getFlag(String flagKey) {
        return flags.get(flagKey);
    }

    public long snapshotVersion() {
        return version;
    }
}
