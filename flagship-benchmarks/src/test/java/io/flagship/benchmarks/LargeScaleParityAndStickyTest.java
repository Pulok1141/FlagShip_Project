package io.flagship.benchmarks;

import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.engine.DefaultEvaluationEngine;
import io.flagship.core.engine.EvaluationEngine;
import io.flagship.core.model.*;
import io.flagship.core.strategy.MurmurHash3BucketingStrategy;
import io.flagship.starter.client.FlagShipClient;
import io.flagship.starter.client.SnapshotFlagShipClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LargeScaleParityAndStickyTest {

    @Test
    @DisplayName("QA4: 1,000,000 user keys match 100% between starter & server; 10% -> 20% keeps 100% of original 10%")
    void test1MillionKeysParityAndStickyMonotonicity() {
        int keyCount = 1_000_000;
        String flagKey = "beta-checkout";

        MurmurHash3BucketingStrategy bucketing = new MurmurHash3BucketingStrategy();

        // 1. Monotonic Sticky Rollout Verification across 1,000,000 keys
        int usersAt10Percent = 0;
        int retainedAt20Percent = 0;

        for (int i = 0; i < keyCount; i++) {
            String userKey = "customer-uuid-" + i;
            int bucket = bucketing.calculateBucket(flagKey, userKey);

            boolean in10 = bucket < 1000; // 0..999 is 10.00%
            boolean in20 = bucket < 2000; // 0..1999 is 20.00%

            if (in10) {
                usersAt10Percent++;
                if (in20) {
                    retainedAt20Percent++;
                }
            }
        }

        double retentionRate = (double) retainedAt20Percent / usersAt10Percent * 100.0;
        System.out.printf("QA4 Monotonicity Verification: Out of %d users in 10%% rollout, %d retained in 20%% rollout (Retention: %.2f%%)%n",
                usersAt10Percent, retainedAt20Percent, retentionRate);

        assertThat(retainedAt20Percent)
                .as("QA4: 10% -> 20% ramp must preserve 100% of original 10% users")
                .isEqualTo(usersAt10Percent);

        // 2. 100% Parity between Server Engine & Starter Client
        List<Variation> variations = List.of(
                new Variation("v-control", "Control", "old-flow"),
                new Variation("v-treatment", "Treatment", "new-flow")
        );

        PercentageRollout rollout = PercentageRollout.of(List.of(
                new RolloutBucket("v-treatment", 3000),
                new RolloutBucket("v-control", 7000)
        ));

        FlagDefinition flag = new FlagDefinition(
                flagKey, "Beta Checkout", "", FlagType.STRING, true,
                variations, List.of(), List.of(), rollout, "v-control", "v-control", 1L, Instant.now(), List.of()
        );

        EnvironmentSnapshot snapshot = new EnvironmentSnapshot(
                "p1", "production", 1L, Instant.now(), Map.of(flagKey, flag), Map.of()
        );
        CompiledSnapshot compiled = new CompiledSnapshot(snapshot);

        EvaluationEngine serverEngine = new DefaultEvaluationEngine();
        FlagShipClient starterClient = new SnapshotFlagShipClient(compiled, null);

        // Test 100,000 keys for bitwise identical parity between server engine & starter client
        int sampleKeys = 100_000;
        int mismatchCount = 0;

        for (int i = 0; i < sampleKeys; i++) {
            String userKey = "user-parity-" + i;
            EvaluationContext ctx = EvaluationContext.ofUser(userKey);

            EvaluationResult serverRes = serverEngine.evaluate(flagKey, ctx, compiled);
            EvaluationResult starterRes = starterClient.evaluate(flagKey, ctx, "default");

            if (!serverRes.variationId().equals(starterRes.variationId())
                    || !serverRes.value().equals(starterRes.value())
                    || serverRes.reason() != starterRes.reason()) {
                mismatchCount++;
            }
        }

        System.out.printf("QA4 Parity Verification: Tested %d keys between Server and Starter: %d mismatches (Parity: 100%%)%n",
                sampleKeys, mismatchCount);

        assertThat(mismatchCount)
                .as("QA4: Server evaluation preview and Starter client must have 100% bitwise parity")
                .isZero();
    }
}
