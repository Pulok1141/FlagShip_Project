package io.flagship.core.ast;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.flagship.core.visitor.RuleNodeVisitor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Composite node in the rule AST representing logical AND / OR branches.
 */
public final class CompositeRuleNode implements RuleNode {

    private final LogicalOperator operator;
    private final List<RuleNode> children;

    @JsonCreator
    public CompositeRuleNode(
            @JsonProperty("operator") LogicalOperator operator,
            @JsonProperty("children") List<RuleNode> children
    ) {
        this.operator = Objects.requireNonNull(operator, "operator must not be null");
        this.children = children != null ? Collections.unmodifiableList(children) : Collections.emptyList();
    }

    public LogicalOperator getOperator() {
        return operator;
    }

    public List<RuleNode> getChildren() {
        return children;
    }

    @Override
    public <T> T accept(RuleNodeVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public static CompositeRuleNode and(RuleNode... nodes) {
        return new CompositeRuleNode(LogicalOperator.AND, List.of(nodes));
    }

    public static CompositeRuleNode or(RuleNode... nodes) {
        return new CompositeRuleNode(LogicalOperator.OR, List.of(nodes));
    }

    public static CompositeRuleNode of(LogicalOperator operator, List<RuleNode> children) {
        return new CompositeRuleNode(operator, children);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompositeRuleNode that = (CompositeRuleNode) o;
        return operator == that.operator && Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operator, children);
    }

    @Override
    public String toString() {
        return "CompositeRuleNode{" +
                "operator=" + operator +
                ", children=" + children +
                '}';
    }
}
