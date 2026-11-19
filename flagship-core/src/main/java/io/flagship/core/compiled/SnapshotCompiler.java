package io.flagship.core.compiled;

import io.flagship.core.model.EnvironmentSnapshot;

/**
 * Utility to compile raw environment snapshots into optimized CompiledSnapshot instances.
 */
public final class SnapshotCompiler {

    private SnapshotCompiler() {}

    public static CompiledSnapshot compile(EnvironmentSnapshot snapshot) {
        if (snapshot == null) {
            return CompiledSnapshot.empty("unknown", "unknown");
        }
        return new CompiledSnapshot(snapshot);
    }
}
