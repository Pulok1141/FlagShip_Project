package io.flagship.core.strategy;

import java.util.Objects;

/**
 * Concrete Strategy implementing deterministic MurmurHash3 bucketing (FR3).
 * MurmurHash3(flagKey + ":" + userKey) mapped unsigned into [0, 9999].
 * Guarantees monotonic sticky rollouts: ramping from 10% to 20% retains 100% of the initial 10%.
 */
public final class MurmurHash3BucketingStrategy implements BucketingStrategy {

    public static final MurmurHash3BucketingStrategy INSTANCE = new MurmurHash3BucketingStrategy();

    @Override
    public int calculateBucket(String flagKey, String userKey) {
        Objects.requireNonNull(flagKey, "flagKey must not be null");
        String effectiveUserKey = (userKey != null && !userKey.isBlank()) ? userKey : "anonymous";
        String combined = flagKey + ":" + effectiveUserKey;
        int hash = MurmurHash3.hash32(combined);
        return Integer.remainderUnsigned(hash, 10000);
    }
}
