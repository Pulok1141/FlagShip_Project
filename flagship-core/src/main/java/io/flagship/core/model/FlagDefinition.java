package io.flagship.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Complete immutable configuration for a feature flag.
 */
public record FlagDefinition(
        @JsonProperty("key") String key,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("type") FlagType type,
        @JsonProperty("enabled") boolean enabled,
        @JsonProperty("variations") List<Variation> variations,
        @JsonProperty("targets") List<TargetRule> targets,
        @JsonProperty("rules") List<SegmentRule> rules,
        @JsonProperty("rollout") PercentageRollout rollout,
        @JsonProperty("defaultVariationId") String defaultVariationId,
        @JsonProperty("offVariationId") String offVariationId,
        @JsonProperty("version") long version,
        @JsonProperty("updatedAt") Instant updatedAt,
        @JsonProperty("tags") List<String> tags
) {
    @JsonCreator
    public FlagDefinition {
        Objects.requireNonNull(key, "Flag key must not be null");
        Objects.requireNonNull(type, "Flag type must not be null");
        Objects.requireNonNull(defaultVariationId, "defaultVariationId must not be null");
        if (name == null) {
            name = key;
        }
        variations = variations != null ? Collections.unmodifiableList(variations) : Collections.emptyList();
        targets = targets != null ? Collections.unmodifiableList(targets) : Collections.emptyList();
        rules = rules != null ? Collections.unmodifiableList(rules) : Collections.emptyList();
        tags = tags != null ? Collections.unmodifiableList(tags) : Collections.emptyList();
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    public Variation findVariation(String variationId) {
        if (variationId == null) return null;
        for (Variation v : variations) {
            if (v.id().equals(variationId)) {
                return v;
            }
        }
        return null;
    }

    public Variation getDefaultVariation() {
        return findVariation(defaultVariationId);
    }

    public Variation getOffVariation() {
        if (offVariationId != null) {
            Variation v = findVariation(offVariationId);
            if (v != null) return v;
        }
        return getDefaultVariation();
    }
}
