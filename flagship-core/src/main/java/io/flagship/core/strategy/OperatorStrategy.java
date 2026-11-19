package io.flagship.core.strategy;

import java.util.List;

/**
 * Strategy pattern interface for evaluating an attribute against target values.
 */
@FunctionalInterface
public interface OperatorStrategy {

    /**
     * Evaluate an attribute against the target values defined in a rule condition.
     *
     * @param attributeValue the attribute value from the EvaluationContext
     * @param targetValues the target values configured in the rule
     * @return true if the condition is satisfied, false otherwise
     */
    boolean evaluate(Object attributeValue, List<String> targetValues);
}
