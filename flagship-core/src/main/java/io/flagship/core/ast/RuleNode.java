package io.flagship.core.ast;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.flagship.core.visitor.RuleNodeVisitor;

/**
 * Composite pattern root interface for rule condition trees.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "nodeType",
        defaultImpl = LeafConditionNode.class
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CompositeRuleNode.class, name = "composite"),
        @JsonSubTypes.Type(value = LeafConditionNode.class, name = "leaf")
})
public interface RuleNode {

    /**
     * Accept a visitor for AST traversal, evaluation, or validation.
     *
     * @param visitor the visitor
     * @param <T> the return type
     * @return visitor result
     */
    <T> T accept(RuleNodeVisitor<T> visitor);
}
