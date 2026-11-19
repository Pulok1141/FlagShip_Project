package io.flagship.core.strategy;

/**
 * Strategy pattern interface for deterministic user bucketing into [0, 9999].
 */
public interface BucketingStrategy {

    /**
     * Map a flag key and user key to an unsigned bucket in the range [0, 9999].
     * Represents 0.00% to 99.99% with 0.01% granularity.
     *
     * @param flagKey feature flag key
     * @param userKey individual user key
     * @return integer in [0, 9999]
     */
    int calculateBucket(String flagKey, String userKey);
}
