package io.flagship.core.visitor;

import io.flagship.core.ast.CompositeRuleNode;
import io.flagship.core.ast.LeafConditionNode;
import io.flagship.core.ast.LogicalOperator;
import io.flagship.core.ast.RuleNode;
import io.flagship.core.model.EvaluationContext;
import io.flagship.core.strategy.OperatorRegistry;
import io.flagship.core.strategy.OperatorStrategy;

import java.util.Objects;

/**
 * Visitor implementation that evaluates a rule AST against an EvaluationContext.
 */
public final class RuleEvaluationVisitor implements RuleNodeVisitor<Boolean> {

    private final EvaluationContext context;
    private final OperatorRegistry operatorRegistry;

    public RuleEvaluationVisitor(EvaluationContext context) {
        this(context, OperatorRegistry.getDefault());
    }

    public RuleEvaluationVisitor(EvaluationContext context, OperatorRegistry operatorRegistry) {
        this.context = Objects.requireNonNull(context, "context must not be null");
        this.operatorRegistry = Objects.requireNonNull(operatorRegistry, "operatorRegistry must not be null");
    }

    @Override
    public Boolean visit(CompositeRuleNode node) {
        if (node.getChildren().isEmpty()) {
            return true;
        }

        if (node.getOperator() == LogicalOperator.AND) {
            for (RuleNode child : node.getChildren()) {
                if (!Boolean.TRUE.equals(child.accept(this))) {
                    return false; // Short-circuit AND
                }
            }
            return true;
        } else if (node.getOperator() == LogicalOperator.OR) {
            for (RuleNode child : node.getChildren()) {
                if (Boolean.TRUE.equals(child.accept(this))) {
                    return true; // Short-circuit OR
                }
            }
            return false;
        }

        return false;
    }

    @Override
    public Boolean visit(LeafConditionNode node) {
        Object attrValue = context.getAttribute(node.getAttribute());
        OperatorStrategy strategy = operatorRegistry.getStrategy(node.getOperator());
        return strategy.evaluate(attrValue, node.getValues());
    }
}
