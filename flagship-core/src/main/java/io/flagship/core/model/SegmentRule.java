package io.flagship.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.flagship.core.ast.RuleNode;

import java.util.Objects;

/**
 * A targeted rule associated with a flag, evaluated via the Composite AST (FR2 step 3).
 */
public record SegmentRule(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("condition") RuleNode condition,
        @JsonProperty("variationId") String variationId
) {
    @JsonCreator
    public SegmentRule {
        Objects.requireNonNull(id, "Rule id must not be null");
        Objects.requireNonNull(condition, "Rule condition must not be null");
        Objects.requireNonNull(variationId, "variationId must not be null");
        if (name == null) {
            name = id;
        }
    }
}
