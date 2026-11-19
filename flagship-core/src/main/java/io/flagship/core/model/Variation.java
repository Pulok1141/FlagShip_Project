package io.flagship.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Represents a discrete flag variation with an identifier, display name, and typed value.
 */
public record Variation(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("value") Object value
) {
    @JsonCreator
    public Variation {
        Objects.requireNonNull(id, "Variation id must not be null");
        if (name == null) {
            name = id;
        }
    }
}
