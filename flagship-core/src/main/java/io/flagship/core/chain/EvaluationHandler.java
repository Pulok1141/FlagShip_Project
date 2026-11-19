package io.flagship.core.chain;

import io.flagship.core.compiled.CompiledFlag;
import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.model.EvaluationContext;
import io.flagship.core.model.EvaluationReason;
import io.flagship.core.model.EvaluationResult;

/**
 * Abstract Handler in the Chain of Responsibility pattern for feature flag evaluation (FR2).
 */
public abstract class EvaluationHandler {

    private EvaluationHandler next;

    public EvaluationHandler setNext(EvaluationHandler next) {
        this.next = next;
        return next;
    }

    public EvaluationHandler getNext() {
        return next;
    }

    /**
     * Handle the evaluation step. If this handler determines the variation, it returns the result.
     * Otherwise, it delegates to {@link #next(EvaluationContext, CompiledFlag, CompiledSnapshot)}.
     *
     * @param context the user context
     * @param flag the pre-compiled flag
     * @param snapshot the compiled environment snapshot
     * @return the evaluation result
     */
    public abstract EvaluationResult handle(
            EvaluationContext context,
            CompiledFlag flag,
            CompiledSnapshot snapshot
    );

    protected EvaluationResult next(
            EvaluationContext context,
            CompiledFlag flag,
            CompiledSnapshot snapshot
    ) {
        if (next != null) {
            return next.handle(context, flag, snapshot);
        }
        return EvaluationResult.of(
                flag.key(),
                flag.defaultVariation(),
                EvaluationReason.DEFAULT,
                null,
                0L
        );
    }
}
