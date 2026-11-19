package io.flagship.core.engine;

import io.flagship.core.compiled.CompiledFlag;
import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.model.EvaluationContext;
import io.flagship.core.model.EvaluationResult;

import java.util.Map;

/**
 * Pure Java evaluation engine contract shared by server and starter.
 */
public interface EvaluationEngine {

    /**
     * Evaluate a single flag by key against the provided compiled snapshot and context.
     *
     * @param flagKey flag identifier
     * @param context evaluation context containing userKey and attributes
     * @param snapshot compiled snapshot
     * @return non-null evaluation result
     */
    EvaluationResult evaluate(String flagKey, EvaluationContext context, CompiledSnapshot snapshot);

    /**
     * Evaluate a pre-compiled flag against context and snapshot.
     */
    EvaluationResult evaluate(CompiledFlag flag, EvaluationContext context, CompiledSnapshot snapshot);

    /**
     * Evaluate all flags in the snapshot for the given context.
     */
    Map<String, EvaluationResult> evaluateAll(EvaluationContext context, CompiledSnapshot snapshot);
}
