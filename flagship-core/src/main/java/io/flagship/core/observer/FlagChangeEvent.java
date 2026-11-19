package io.flagship.core.observer;

import io.flagship.core.model.FlagDefinition;

import java.time.Instant;
import java.util.Objects;

/**
 * Event fired when a feature flag configuration changes.
 */
public record FlagChangeEvent(
        String flagKey,
        FlagDefinition oldFlag,
        FlagDefinition newFlag,
        Instant timestamp
) {
    public FlagChangeEvent {
        Objects.requireNonNull(flagKey, "flagKey must not be null");
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
