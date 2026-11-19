package io.flagship.core.chain;

import io.flagship.core.ast.LeafConditionNode;
import io.flagship.core.ast.OperatorType;
import io.flagship.core.compiled.CompiledFlag;
import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ChainOfResponsibilityTest {

    private EvaluationPipeline pipeline;
    private Variation varOff;
    private Variation varTarget;
    private Variation varRule;
    private Variation varRollout;
    private Variation varDefault;

    @BeforeEach
    void setUp() {
        pipeline = new EvaluationPipeline();
        varOff = new Variation("var-off", "Off", false);
        varTarget = new Variation("var-target", "Target Whitelist", "target-val");
        varRule = new Variation("var-rule", "Beta Segment", "rule-val");
        varRollout = new Variation("var-rollout", "Canary 50%", "rollout-val");
        varDefault = new Variation("var-default", "Default", "default-val");
    }

    private FlagDefinition createFullFlag(boolean enabled) {
        return new FlagDefinition(
                "checkout-v2",
                "Checkout V2",
                "Flag for testing chain of responsibility",
                FlagType.STRING,
                enabled,
                List.of(varOff, varTarget, varRule, varRollout, varDefault),
                List.of(new TargetRule(varTarget.id(), Set.of("vip-user-1", "vip-user-2"))),
                List.of(new SegmentRule("rule-1", "VIP Plan Rule",
                        LeafConditionNode.of("tier", OperatorType.EQUALS, "gold"), varRule.id())),
                PercentageRollout.of(List.of(new RolloutBucket(varRollout.id(), 10000))), // 100% rollout to test
                varDefault.id(),
                varOff.id(),
                1L,
                Instant.now(),
                List.of("checkout")
        );
    }

    @Test
    @DisplayName("Step 1: Kill switch overrides user targets, rules, and rollouts")
    void step1KillSwitchOverridesAll() {
        FlagDefinition flag = createFullFlag(false); // disabled
        CompiledFlag compiledFlag = new CompiledFlag(flag);
        CompiledSnapshot snapshot = new CompiledSnapshot(new EnvironmentSnapshot("p1", "dev", 1L, Instant.now(), Map.of(flag.key(), flag), Map.of()));

        // User is both targeted AND matches rule tier=gold
        EvaluationContext ctx = EvaluationContext.builder()
                .userKey("vip-user-1")
                .attribute("tier", "gold")
                .build();

        EvaluationResult result = pipeline.evaluate(ctx, compiledFlag, snapshot);
        assertThat(result.reason()).isEqualTo(EvaluationReason.KILL_SWITCH);
        assertThat(result.variationId()).isEqualTo(varOff.id());
    }

    @Test
    @DisplayName("Step 2: Individual user target overrides rules and rollouts when flag is enabled")
    void step2UserTargetOverridesRulesAndRollout() {
        FlagDefinition flag = createFullFlag(true); // enabled
        CompiledFlag compiledFlag = new CompiledFlag(flag);
        CompiledSnapshot snapshot = new CompiledSnapshot(new EnvironmentSnapshot("p1", "dev", 1L, Instant.now(), Map.of(flag.key(), flag), Map.of()));

        // User is targeted AND matches rule tier=gold
        EvaluationContext ctx = EvaluationContext.builder()
                .userKey("vip-user-1")
                .attribute("tier", "gold")
                .build();

        EvaluationResult result = pipeline.evaluate(ctx, compiledFlag, snapshot);
        assertThat(result.reason()).isEqualTo(EvaluationReason.TARGET_MATCH);
        assertThat(result.variationId()).isEqualTo(varTarget.id());
    }

    @Test
    @DisplayName("Step 3: Segment rules override percentage rollout when user is not directly targeted")
    void step3SegmentRulesOverrideRollout() {
        FlagDefinition flag = createFullFlag(true); // enabled
        CompiledFlag compiledFlag = new CompiledFlag(flag);
        CompiledSnapshot snapshot = new CompiledSnapshot(new EnvironmentSnapshot("p1", "dev", 1L, Instant.now(), Map.of(flag.key(), flag), Map.of()));

        // Non-targeted user, but has tier=gold
        EvaluationContext ctx = EvaluationContext.builder()
                .userKey("regular-user-99")
                .attribute("tier", "gold")
                .build();

        EvaluationResult result = pipeline.evaluate(ctx, compiledFlag, snapshot);
        assertThat(result.reason()).isEqualTo(EvaluationReason.RULE_MATCH);
        assertThat(result.variationId()).isEqualTo(varRule.id());
        assertThat(result.matchedRuleId()).isEqualTo("rule-1");
    }

    @Test
    @DisplayName("Step 4: Percentage rollout applies when no user target or segment rule matches")
    void step4PercentageRolloutApplies() {
        FlagDefinition flag = createFullFlag(true);
        CompiledFlag compiledFlag = new CompiledFlag(flag);
        CompiledSnapshot snapshot = new CompiledSnapshot(new EnvironmentSnapshot("p1", "dev", 1L, Instant.now(), Map.of(flag.key(), flag), Map.of()));

        // Non-targeted user, tier=silver (no rule match) -> 100% rollout matches
        EvaluationContext ctx = EvaluationContext.builder()
                .userKey("regular-user-99")
                .attribute("tier", "silver")
                .build();

        EvaluationResult result = pipeline.evaluate(ctx, compiledFlag, snapshot);
        assertThat(result.reason()).isEqualTo(EvaluationReason.PERCENTAGE_ROLLOUT);
        assertThat(result.variationId()).isEqualTo(varRollout.id());
    }

    @Test
    @DisplayName("Step 5: Fallthrough returns default variation when no rollout matches")
    void step5DefaultVariationFallthrough() {
        // Flag with 0% rollout
        FlagDefinition flag = new FlagDefinition(
                "checkout-v2",
                "Checkout V2",
                "Flag with empty rollout",
                FlagType.STRING,
                true,
                List.of(varOff, varTarget, varRule, varRollout, varDefault),
                List.of(), // no targets
                List.of(), // no rules
                PercentageRollout.of(List.of()), // 0% rollout
                varDefault.id(),
                varOff.id(),
                1L,
                Instant.now(),
                List.of()
        );
        CompiledFlag compiledFlag = new CompiledFlag(flag);
        CompiledSnapshot snapshot = new CompiledSnapshot(new EnvironmentSnapshot("p1", "dev", 1L, Instant.now(), Map.of(flag.key(), flag), Map.of()));

        EvaluationContext ctx = EvaluationContext.ofUser("any-user");
        EvaluationResult result = pipeline.evaluate(ctx, compiledFlag, snapshot);

        assertThat(result.reason()).isEqualTo(EvaluationReason.DEFAULT);
        assertThat(result.variationId()).isEqualTo(varDefault.id());
    }
}
