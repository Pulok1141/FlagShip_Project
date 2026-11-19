package io.flagship.core.chain;

import io.flagship.core.compiled.CompiledFlag;
import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.model.EvaluationContext;
import io.flagship.core.model.EvaluationReason;
import io.flagship.core.model.EvaluationResult;
import io.flagship.core.model.SegmentRule;
import io.flagship.core.model.Variation;
import io.flagship.core.visitor.RuleEvaluationVisitor;

/**
 * Step 3 in Chain of Responsibility: Segment Rules.
 * Evaluates rule conditions via Composite + Visitor AST in order.
 */
public final class SegmentRuleHandler extends EvaluationHandler {

    @Override
    public EvaluationResult handle(
            EvaluationContext context,
            CompiledFlag flag,
            CompiledSnapshot snapshot
    ) {
        if (!flag.rules().isEmpty()) {
            RuleEvaluationVisitor visitor = new RuleEvaluationVisitor(context);
            for (SegmentRule rule : flag.rules()) {
                if (rule.condition() != null) {
                    Boolean matched = rule.condition().accept(visitor);
                    if (Boolean.TRUE.equals(matched)) {
                        Variation variation = flag.findVariation(rule.variationId());
                        if (variation != null) {
                            return EvaluationResult.of(
                                    flag.key(),
                                    variation,
                                    EvaluationReason.RULE_MATCH,
                                    rule.id(),
                                    0L
                            );
                        }
                    }
                }
            }
        }
        return next(context, flag, snapshot);
    }
}
