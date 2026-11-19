package io.flagship.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.flagship.core.ast.RuleNode;

import java.util.Objects;

/**
 * Reusable user segment definition.
 */
public record SegmentDefinition(
        @JsonProperty("key") String key,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("condition") RuleNode condition
) {
    @JsonCreator
    public SegmentDefinition {
        Objects.requireNonNull(key, "Segment key must not be null");
        Objects.requireNonNull(condition, "Segment condition must not be null");
        if (name == null) {
            name = key;
        }
    }
}
