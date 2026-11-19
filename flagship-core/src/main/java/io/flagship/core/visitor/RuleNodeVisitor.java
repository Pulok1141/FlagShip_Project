package io.flagship.core.visitor;

import io.flagship.core.ast.CompositeRuleNode;
import io.flagship.core.ast.LeafConditionNode;

/**
 * Visitor pattern interface for traversing rule AST nodes.
 *
 * @param <T> The return type of the visit operations
 */
public interface RuleNodeVisitor<T> {

    T visit(CompositeRuleNode node);

    T visit(LeafConditionNode node);
}
