package io.flagship.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * A discrete variation bucket in a percentage rollout.
 * Weight is expressed in basis points: 0 to 10000 (where 10000 = 100.00%).
 */
public record RolloutBucket(
        @JsonProperty("variationId") String variationId,
        @JsonProperty("weight") int weight
) {
    @JsonCreator
    public RolloutBucket {
        Objects.requireNonNull(variationId, "variationId must not be null");
        if (weight < 0 || weight > 10000) {
            throw new IllegalArgumentException("Weight must be between 0 and 10000 basis points (got " + weight + ")");
        }
    }

    public static RolloutBucket of(String variationId, int basisPoints) {
        return new RolloutBucket(variationId, basisPoints);
    }

    public static RolloutBucket ofPercentage(String variationId, double percentage) {
        int bp = (int) Math.round(percentage * 100.0);
        return new RolloutBucket(variationId, bp);
    }
}
