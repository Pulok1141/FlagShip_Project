package io.flagship.core.chain;

import io.flagship.core.compiled.CompiledFlag;
import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.model.EvaluationContext;
import io.flagship.core.model.EvaluationReason;
import io.flagship.core.model.EvaluationResult;
import io.flagship.core.model.TargetRule;
import io.flagship.core.model.Variation;

/**
 * Step 2 in Chain of Responsibility: Individual User Targets.
 * Checks whether the userKey matches explicit user whitelist rules.
 */
public final class UserTargetHandler extends EvaluationHandler {

    @Override
    public EvaluationResult handle(
            EvaluationContext context,
            CompiledFlag flag,
            CompiledSnapshot snapshot
    ) {
        String userKey = context.getUserKey();
        if (userKey != null && !userKey.isBlank() && !flag.targets().isEmpty()) {
            for (TargetRule target : flag.targets()) {
                if (target.userKeys().contains(userKey)) {
                    Variation variation = flag.findVariation(target.variationId());
                    if (variation != null) {
                        return EvaluationResult.of(
                                flag.key(),
                                variation,
                                EvaluationReason.TARGET_MATCH,
                                null,
                                0L
                        );
                    }
                }
            }
        }
        return next(context, flag, snapshot);
    }
}
