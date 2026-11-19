package io.flagship.core.ast;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.flagship.core.visitor.RuleNodeVisitor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Leaf node in the rule AST evaluating an attribute against an operator and target values.
 */
public final class LeafConditionNode implements RuleNode {

    private final String attribute;
    private final OperatorType operator;
    private final List<String> values;

    @JsonCreator
    public LeafConditionNode(
            @JsonProperty("attribute") String attribute,
            @JsonProperty("operator") OperatorType operator,
            @JsonProperty("values") List<String> values
    ) {
        this.attribute = Objects.requireNonNull(attribute, "attribute must not be null");
        this.operator = Objects.requireNonNull(operator, "operator must not be null");
        this.values = values != null ? Collections.unmodifiableList(values) : Collections.emptyList();
    }

    public String getAttribute() {
        return attribute;
    }

    public OperatorType getOperator() {
        return operator;
    }

    public List<String> getValues() {
        return values;
    }

    @Override
    public <T> T accept(RuleNodeVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public static LeafConditionNode of(String attribute, OperatorType operator, String... values) {
        return new LeafConditionNode(attribute, operator, List.of(values));
    }

    public static LeafConditionNode of(String attribute, OperatorType operator, List<String> values) {
        return new LeafConditionNode(attribute, operator, values);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeafConditionNode that = (LeafConditionNode) o;
        return Objects.equals(attribute, that.attribute) &&
                operator == that.operator &&
                Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attribute, operator, values);
    }

    @Override
    public String toString() {
        return "LeafConditionNode{" +
                "attribute='" + attribute + '\'' +
                ", operator=" + operator +
                ", values=" + values +
                '}';
    }
}
