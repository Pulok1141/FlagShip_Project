package io.flagship.core.compiled;

import io.flagship.core.model.EnvironmentSnapshot;
import io.flagship.core.model.FlagDefinition;
import io.flagship.core.model.SegmentDefinition;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable in-memory snapshot with pre-compiled flags and segment indexes.
 * Designed to be held in an AtomicReference for zero-lock, sub-50µs reads.
 */
public final class CompiledSnapshot {

    private final EnvironmentSnapshot rawSnapshot;
    private final Map<String, CompiledFlag> compiledFlags;
    private final Map<String, SegmentDefinition> segments;

    public CompiledSnapshot(EnvironmentSnapshot rawSnapshot) {
        this.rawSnapshot = Objects.requireNonNull(rawSnapshot, "rawSnapshot must not be null");

        Map<String, CompiledFlag> cFlags = new HashMap<>();
        if (rawSnapshot.flags() != null) {
            for (Map.Entry<String, FlagDefinition> entry : rawSnapshot.flags().entrySet()) {
                cFlags.put(entry.getKey(), new CompiledFlag(entry.getValue()));
            }
        }
        this.compiledFlags = Collections.unmodifiableMap(cFlags);
        this.segments = rawSnapshot.segments() != null ?
                Collections.unmodifiableMap(new HashMap<>(rawSnapshot.segments())) :
                Collections.emptyMap();
    }

    public static CompiledSnapshot empty(String projectId, String environmentKey) {
        return new CompiledSnapshot(EnvironmentSnapshot.empty(projectId, environmentKey));
    }

    public static CompiledSnapshot empty() {
        return empty("unknown", "unknown");
    }

    public Map<String, CompiledFlag> flags() {
        return compiledFlags;
    }

    public EnvironmentSnapshot rawSnapshot() {
        return rawSnapshot;
    }

    public String projectId() {
        return rawSnapshot.projectId();
    }

    public String environmentKey() {
        return rawSnapshot.environmentKey();
    }

    public long version() {
        return rawSnapshot.version();
    }

    public Instant updatedAt() {
        return rawSnapshot.updatedAt();
    }

    public CompiledFlag getCompiledFlag(String flagKey) {
        return compiledFlags.get(flagKey);
    }

    public Map<String, CompiledFlag> allCompiledFlags() {
        return compiledFlags;
    }

    public SegmentDefinition getSegment(String segmentKey) {
        return segments.get(segmentKey);
    }

    public int flagCount() {
        return compiledFlags.size();
    }
}
