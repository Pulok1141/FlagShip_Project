package io.flagship.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Explicit user target rule mapping specific userKeys directly to a variation (FR2 step 2).
 */
public record TargetRule(
        @JsonProperty("variationId") String variationId,
        @JsonProperty("userKeys") Set<String> userKeys
) {
    @JsonCreator
    public TargetRule {
        Objects.requireNonNull(variationId, "variationId must not be null");
        userKeys = userKeys != null ? Collections.unmodifiableSet(userKeys) : Collections.emptySet();
    }

    public static TargetRule of(String variationId, String... keys) {
        return new TargetRule(variationId, Set.of(keys));
    }
}
