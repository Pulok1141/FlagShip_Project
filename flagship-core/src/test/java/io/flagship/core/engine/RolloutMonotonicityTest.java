package io.flagship.core.engine;

import io.flagship.core.compiled.CompiledFlag;
import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RolloutMonotonicityTest {

    private final DefaultEvaluationEngine engine = new DefaultEvaluationEngine();

    @Test
    @DisplayName("FR3 / QA4: 1,000,000-key sticky rollout monotonicity test (10% -> 20% ramp retains 100% of original users)")
    void testOneMillionKeyMonotonicity() {
        Variation varOn = new Variation("on", "Enabled", true);
        Variation varOff = new Variation("off", "Disabled", false);

        // Flag at 10% rollout (1000 basis points)
        FlagDefinition flag10 = new FlagDefinition(
                "ramp-test-flag",
                "Ramp Test Flag",
                "Testing 1M-key sticky monotonicity",
                FlagType.BOOLEAN,
                true,
                List.of(varOn, varOff),
                List.of(),
                List.of(),
                PercentageRollout.of(List.of(new RolloutBucket("on", 1000))),
                "off",
                "off",
                1L,
                Instant.now(),
                List.of()
        );

        // Flag ramped to 20% rollout (2000 basis points)
        FlagDefinition flag20 = new FlagDefinition(
                "ramp-test-flag",
                "Ramp Test Flag",
                "Testing 1M-key sticky monotonicity",
                FlagType.BOOLEAN,
                true,
                List.of(varOn, varOff),
                List.of(),
                List.of(),
                PercentageRollout.of(List.of(new RolloutBucket("on", 2000))),
                "off",
                "off",
                2L,
                Instant.now(),
                List.of()
        );

        CompiledFlag compiled10 = new CompiledFlag(flag10);
        CompiledFlag compiled20 = new CompiledFlag(flag20);

        CompiledSnapshot snap10 = new CompiledSnapshot(new EnvironmentSnapshot("p1", "prod", 1L, Instant.now(), Map.of(flag10.key(), flag10), Map.of()));
        CompiledSnapshot snap20 = new CompiledSnapshot(new EnvironmentSnapshot("p1", "prod", 2L, Instant.now(), Map.of(flag20.key(), flag20), Map.of()));

        int totalKeys = 1_000_000;
        int count10 = 0;
        int count20 = 0;
        int retainedCount = 0;

        for (int i = 0; i < totalKeys; i++) {
            String userKey = "user-" + i;
            EvaluationContext ctx = EvaluationContext.ofUser(userKey);

            EvaluationResult res10 = engine.evaluate(compiled10, ctx, snap10);
            EvaluationResult res20 = engine.evaluate(compiled20, ctx, snap20);

            boolean onAt10 = "on".equals(res10.variationId());
            boolean onAt20 = "on".equals(res20.variationId());

            if (onAt10) {
                count10++;
                // Strict Monotonicity Check: If user was ON at 10%, user MUST be ON at 20%!
                if (onAt20) {
                    retainedCount++;
                }
            }

            if (onAt20) {
                count20++;
            }
        }

        // 1. Assert 100% retention
        assertThat(retainedCount)
                .as("100% of users in the 10% bucket must be retained when ramping to 20%")
                .isEqualTo(count10);

        // 2. Assert uniform statistical distribution around 10% and 20%
        double pct10 = (count10 * 100.0) / totalKeys;
        double pct20 = (count20 * 100.0) / totalKeys;

        assertThat(pct10)
                .as("10% rollout should be between 9.5% and 10.5%")
                .isBetween(9.5, 10.5);

        assertThat(pct20)
                .as("20% rollout should be between 19.5% and 20.5%")
                .isBetween(19.5, 20.5);
    }
}
