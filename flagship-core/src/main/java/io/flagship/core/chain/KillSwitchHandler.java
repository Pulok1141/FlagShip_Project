package io.flagship.core.chain;

import io.flagship.core.compiled.CompiledFlag;
import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.model.EvaluationContext;
import io.flagship.core.model.EvaluationReason;
import io.flagship.core.model.EvaluationResult;

/**
 * Step 1 in Chain of Responsibility: Kill Switch.
 * If flag is disabled, immediately returns the configured off-variation.
 */
public final class KillSwitchHandler extends EvaluationHandler {

    @Override
    public EvaluationResult handle(
            EvaluationContext context,
            CompiledFlag flag,
            CompiledSnapshot snapshot
    ) {
        if (!flag.isEnabled()) {
            return EvaluationResult.of(
                    flag.key(),
                    flag.offVariation(),
                    EvaluationReason.KILL_SWITCH,
                    null,
                    0L
            );
        }
        return next(context, flag, snapshot);
    }
}
