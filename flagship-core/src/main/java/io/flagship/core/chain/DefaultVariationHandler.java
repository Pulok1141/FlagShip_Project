package io.flagship.core.chain;

import io.flagship.core.compiled.CompiledFlag;
import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.model.EvaluationContext;
import io.flagship.core.model.EvaluationReason;
import io.flagship.core.model.EvaluationResult;

/**
 * Step 5 in Chain of Responsibility: Default Variation.
 * Terminal handler returning the configured default variation when all other rules pass through.
 */
public final class DefaultVariationHandler extends EvaluationHandler {

    @Override
    public EvaluationResult handle(
            EvaluationContext context,
            CompiledFlag flag,
            CompiledSnapshot snapshot
    ) {
        return EvaluationResult.of(
                flag.key(),
                flag.defaultVariation(),
                EvaluationReason.DEFAULT,
                null,
                0L
        );
    }
}
