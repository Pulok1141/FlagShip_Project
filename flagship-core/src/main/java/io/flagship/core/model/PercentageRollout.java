package io.flagship.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Configuration for gradual and sticky percentage rollouts (FR3).
 */
public record PercentageRollout(
        @JsonProperty("buckets") List<RolloutBucket> buckets,
        @JsonProperty("bucketBy") String bucketBy
) {
    @JsonCreator
    public PercentageRollout {
        buckets = buckets != null ? Collections.unmodifiableList(buckets) : Collections.emptyList();
        if (bucketBy == null || bucketBy.isBlank()) {
            bucketBy = "userKey";
        }
    }

    public static PercentageRollout of(List<RolloutBucket> buckets) {
        return new PercentageRollout(buckets, "userKey");
    }

    public static PercentageRollout of(String bucketBy, List<RolloutBucket> buckets) {
        return new PercentageRollout(buckets, bucketBy);
    }
}
