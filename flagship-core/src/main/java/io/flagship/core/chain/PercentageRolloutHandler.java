package io.flagship.core.chain;

import io.flagship.core.compiled.CompiledFlag;
import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.model.EvaluationContext;
import io.flagship.core.model.EvaluationReason;
import io.flagship.core.model.EvaluationResult;
import io.flagship.core.strategy.BucketingStrategy;
import io.flagship.core.strategy.MurmurHash3BucketingStrategy;

import java.util.List;
import java.util.Objects;

/**
 * Step 4 in Chain of Responsibility: Percentage Rollout.
 * Evaluates sticky deterministic bucketing using MurmurHash3 (FR3).
 */
public final class PercentageRolloutHandler extends EvaluationHandler {

    private final BucketingStrategy bucketingStrategy;

    public PercentageRolloutHandler() {
        this(MurmurHash3BucketingStrategy.INSTANCE);
    }

    public PercentageRolloutHandler(BucketingStrategy bucketingStrategy) {
        this.bucketingStrategy = Objects.requireNonNull(bucketingStrategy, "bucketingStrategy must not be null");
    }

    @Override
    public EvaluationResult handle(
            EvaluationContext context,
            CompiledFlag flag,
            CompiledSnapshot snapshot
    ) {
        List<CompiledFlag.CumulativeBucket> cumulativeBuckets = flag.cumulativeRollouts();
        if (!cumulativeBuckets.isEmpty()) {
            Object bucketValue = context.getAttribute(flag.rolloutBucketBy());
            String bucketKey = (bucketValue != null) ? bucketValue.toString() : context.getUserKey();

            int bucket = bucketingStrategy.calculateBucket(flag.key(), bucketKey);

            for (CompiledFlag.CumulativeBucket cb : cumulativeBuckets) {
                if (bucket < cb.threshold()) {
                    return EvaluationResult.of(
                            flag.key(),
                            cb.variation(),
                            EvaluationReason.PERCENTAGE_ROLLOUT,
                            null,
                            0L
                    );
                }
            }
        }
        return next(context, flag, snapshot);
    }
}
