package io.flagship.core.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MurmurHash3Test {

    @Test
    @DisplayName("MurmurHash3 produces deterministic hash values")
    void deterministicHashing() {
        int hash1 = MurmurHash3.hash32("flagship:test-user-123");
        int hash2 = MurmurHash3.hash32("flagship:test-user-123");
        assertThat(hash1).isEqualTo(hash2);

        int hashDifferent = MurmurHash3.hash32("flagship:test-user-456");
        assertThat(hash1).isNotEqualTo(hashDifferent);
    }

    @Test
    @DisplayName("MurmurHash3 bucketing strictly bounds output to [0, 9999]")
    void bucketingBounds() {
        MurmurHash3BucketingStrategy strategy = MurmurHash3BucketingStrategy.INSTANCE;
        for (int i = 0; i < 5000; i++) {
            int bucket = strategy.calculateBucket("checkout-feature", "user-" + i);
            assertThat(bucket).isGreaterThanOrEqualTo(0).isLessThan(10000);
        }
    }
}
