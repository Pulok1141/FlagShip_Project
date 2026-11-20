package io.flagship.benchmarks;

import io.flagship.core.ast.CompositeRuleNode;
import io.flagship.core.ast.LeafConditionNode;
import io.flagship.core.ast.LogicalOperator;
import io.flagship.core.ast.OperatorType;
import io.flagship.core.ast.RuleNode;
import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.engine.DefaultEvaluationEngine;
import io.flagship.core.engine.EvaluationEngine;
import io.flagship.core.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryHeapVerificationTest {

    @Test
    @DisplayName("QA1: 1,000 flags consume <= 20 MB heap memory and 10-rule evaluations take <= 50 µs p99")
    void test1000FlagsHeapAndLatency() {
        // 1. Build 1,000 flags with variations and 10-rule composite condition trees
        Map<String, FlagDefinition> flags = new HashMap<>();

        for (int i = 0; i < 1000; i++) {
            String flagKey = "flag-perf-" + i;

            List<Variation> variations = List.of(
                    new Variation("v-off", "Off", false),
                    new Variation("v-on", "On", true)
            );

            // Construct 10-condition composite rule
            List<RuleNode> conditions = new ArrayList<>();
            conditions.add(new LeafConditionNode("tier", OperatorType.IN, List.of("gold", "platinum", "enterprise")));
            conditions.add(new LeafConditionNode("country", OperatorType.IN, List.of("US", "CA", "GB", "DE")));
            conditions.add(new LeafConditionNode("appVersion", OperatorType.SEMVER_GTE, List.of("2.0.0")));
            conditions.add(new LeafConditionNode("email", OperatorType.REGEX, List.of(".*@flagship\\.io$")));
            conditions.add(new LeafConditionNode("signupDate", OperatorType.DATE_AFTER, List.of("2024-01-01T00:00:00Z")));
            conditions.add(new LeafConditionNode("device", OperatorType.NOT_IN, List.of("legacy-symbian", "win-phone")));
            conditions.add(new LeafConditionNode("betaOptIn", OperatorType.EQUALS, List.of("true")));
            conditions.add(new LeafConditionNode("score", OperatorType.NOT_EQUALS, List.of("0")));
            conditions.add(new LeafConditionNode("role", OperatorType.CONTAINS, List.of("developer")));
            conditions.add(new LeafConditionNode("status", OperatorType.STARTS_WITH, List.of("active")));

            CompositeRuleNode ruleTree = new CompositeRuleNode(LogicalOperator.AND, conditions);
            SegmentRule segmentRule = new SegmentRule("rule-1", "rule-1", ruleTree, "v-on");

            FlagDefinition flag = new FlagDefinition(
                    flagKey, "Perf Flag " + i, "Stress test flag", FlagType.BOOLEAN, true,
                    variations,
                    List.of(new TargetRule("v-on", Set.of("vip-user-" + i))),
                    List.of(segmentRule),
                    PercentageRollout.of(List.of(new RolloutBucket("v-on", 5000), new RolloutBucket("v-off", 5000))),
                    "v-on", "v-off", 1L, Instant.now(), List.of("perf", "stress")
            );

            flags.put(flagKey, flag);
        }

        EnvironmentSnapshot snapshot = new EnvironmentSnapshot(
                "proj-bench", "production", 1L, Instant.now(), flags, Map.of()
        );

        // Force GC before measuring baseline
        System.gc();
        System.gc();
        long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        CompiledSnapshot compiledSnapshot = new CompiledSnapshot(snapshot);

        System.gc();
        System.gc();
        long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        long heapUsedBytes = Math.max(0, memAfter - memBefore);
        double heapUsedMb = heapUsedBytes / (1024.0 * 1024.0);

        System.out.printf("QA1 Heap Verification: 1,000 compiled flags consumed %.2f MB heap%n", heapUsedMb);
        assertThat(heapUsedMb)
                .as("QA1: 1,000 flags heap memory must be strictly <= 20 MB")
                .isLessThanOrEqualTo(20.0);

        // 2. Latency Verification: Evaluate 50,000 times on 10-rule composite condition
        EvaluationEngine engine = new DefaultEvaluationEngine();
        EvaluationContext context = EvaluationContext.builder()
                .userKey("test-user-benchmark")
                .attribute("tier", "enterprise")
                .attribute("country", "US")
                .attribute("appVersion", "2.4.1")
                .attribute("email", "engineer@flagship.io")
                .attribute("signupDate", "2024-06-15T12:00:00Z")
                .attribute("device", "macOS")
                .attribute("betaOptIn", "true")
                .attribute("score", "100")
                .attribute("role", "senior-developer")
                .attribute("status", "active-member")
                .build();

        // Warmup JIT
        for (int i = 0; i < 10000; i++) {
            engine.evaluate("flag-perf-" + (i % 1000), context, compiledSnapshot);
        }

        // Measure latencies
        int sampleCount = 50000;
        long[] durationsNanos = new long[sampleCount];

        for (int i = 0; i < sampleCount; i++) {
            long start = System.nanoTime();
            EvaluationResult res = engine.evaluate("flag-perf-42", context, compiledSnapshot);
            durationsNanos[i] = System.nanoTime() - start;
            assertThat(res.value()).isEqualTo(true);
            assertThat(res.reason()).isEqualTo(EvaluationReason.RULE_MATCH);
        }

        Arrays.sort(durationsNanos);
        long p99Nanos = durationsNanos[(int) (sampleCount * 0.99)];
        double p99Micros = p99Nanos / 1000.0;

        System.out.printf("QA1 Latency Verification: 10-rule evaluation p99 latency = %.2f µs (target <= 50 µs)%n", p99Micros);
        assertThat(p99Micros)
                .as("QA1: 10-rule evaluation p99 must be strictly <= 50 µs")
                .isLessThanOrEqualTo(50.0);
    }
}
