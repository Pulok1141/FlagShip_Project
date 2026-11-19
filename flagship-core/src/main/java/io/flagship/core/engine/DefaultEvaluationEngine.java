package io.flagship.core.engine;

import io.flagship.core.chain.EvaluationPipeline;
import io.flagship.core.compiled.CompiledFlag;
import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.model.EvaluationContext;
import io.flagship.core.model.EvaluationReason;
import io.flagship.core.model.EvaluationResult;
import io.flagship.core.strategy.BucketingStrategy;
import io.flagship.core.strategy.MurmurHash3BucketingStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Standard thread-safe, lock-free evaluation engine using the EvaluationPipeline.
 */
public final class DefaultEvaluationEngine implements EvaluationEngine {

    private final EvaluationPipeline pipeline;

    public DefaultEvaluationEngine() {
        this(new EvaluationPipeline(MurmurHash3BucketingStrategy.INSTANCE));
    }

    public DefaultEvaluationEngine(BucketingStrategy bucketingStrategy) {
        this.pipeline = new EvaluationPipeline(bucketingStrategy);
    }

    public DefaultEvaluationEngine(EvaluationPipeline pipeline) {
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline must not be null");
    }

    @Override
    public EvaluationResult evaluate(String flagKey, EvaluationContext context, CompiledSnapshot snapshot) {
        if (flagKey == null || snapshot == null) {
            return EvaluationResult.error(flagKey != null ? flagKey : "unknown", null, "Null flagKey or snapshot");
        }

        CompiledFlag compiledFlag = snapshot.getCompiledFlag(flagKey);
        if (compiledFlag == null) {
            return EvaluationResult.error(flagKey, null, "Flag not found in snapshot: " + flagKey);
        }

        return evaluate(compiledFlag, context, snapshot);
    }

    @Override
    public EvaluationResult evaluate(CompiledFlag flag, EvaluationContext context, CompiledSnapshot snapshot) {
        if (flag == null) {
            return EvaluationResult.error("unknown", null, "Null compiled flag");
        }
        EvaluationContext effectiveContext = (context != null) ? context : EvaluationContext.ofUser("anonymous");
        return pipeline.evaluate(effectiveContext, flag, snapshot);
    }

    @Override
    public Map<String, EvaluationResult> evaluateAll(EvaluationContext context, CompiledSnapshot snapshot) {
        if (snapshot == null) {
            return Map.of();
        }
        EvaluationContext effectiveContext = (context != null) ? context : EvaluationContext.ofUser("anonymous");
        Map<String, EvaluationResult> results = new HashMap<>();
        for (CompiledFlag flag : snapshot.allCompiledFlags().values()) {
            results.put(flag.key(), pipeline.evaluate(effectiveContext, flag, snapshot));
        }
        return results;
    }
}
