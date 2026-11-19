package io.flagship.core.chain;

import io.flagship.core.compiled.CompiledFlag;
import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.model.EvaluationContext;
import io.flagship.core.model.EvaluationResult;
import io.flagship.core.strategy.BucketingStrategy;
import io.flagship.core.strategy.MurmurHash3BucketingStrategy;

import java.util.Objects;

/**
 * Encapsulates the Chain of Responsibility evaluation pipeline (FR2).
 * Strictly guarantees the evaluation order:
 * 1. Kill Switch
 * 2. Individual User Targets
 * 3. Segment Rules (Composite AST)
 * 4. Percentage Rollout (MurmurHash3)
 * 5. Default Variation
 */
public final class EvaluationPipeline {

    private final EvaluationHandler head;

    public EvaluationPipeline() {
        this(MurmurHash3BucketingStrategy.INSTANCE);
    }

    public EvaluationPipeline(BucketingStrategy bucketingStrategy) {
        // Build chain
        EvaluationHandler killSwitch = new KillSwitchHandler();
        EvaluationHandler userTargets = new UserTargetHandler();
        EvaluationHandler segmentRules = new SegmentRuleHandler();
        EvaluationHandler rollout = new PercentageRolloutHandler(bucketingStrategy);
        EvaluationHandler defaultVar = new DefaultVariationHandler();

        killSwitch.setNext(userTargets);
        userTargets.setNext(segmentRules);
        segmentRules.setNext(rollout);
        rollout.setNext(defaultVar);

        this.head = killSwitch;
    }

    public EvaluationResult evaluate(
            EvaluationContext context,
            CompiledFlag flag,
            CompiledSnapshot snapshot
    ) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(flag, "flag must not be null");

        long startNanos = System.nanoTime();
        EvaluationResult rawResult = head.handle(context, flag, snapshot);
        long durationNanos = System.nanoTime() - startNanos;

        return new EvaluationResult(
                rawResult.flagKey(),
                rawResult.variationId(),
                rawResult.variationName(),
                rawResult.value(),
                rawResult.reason(),
                rawResult.matchedRuleId(),
                durationNanos
        );
    }
}
