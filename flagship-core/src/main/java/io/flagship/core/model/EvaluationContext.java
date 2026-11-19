package io.flagship.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable context for feature flag evaluation.
 * Uses the Builder Pattern (EvaluationContext.builder()) for clean and flexible construction.
 */
public final class EvaluationContext {

    private final String userKey;
    private final Map<String, Object> attributes;

    private EvaluationContext(String userKey, Map<String, Object> attributes) {
        this.userKey = Objects.requireNonNull(userKey, "userKey must not be null");
        this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    @JsonProperty("userKey")
    public String getUserKey() {
        return userKey;
    }

    public String userKey() {
        return userKey;
    }

    public static EvaluationContext anonymous() {
        return ofUser("anonymous");
    }

    public static EvaluationContext of(String userKey, Map<String, Object> attributes) {
        return builder().userKey(userKey).attributes(attributes).build();
    }

    @JsonProperty("attributes")
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Object getAttribute(String key) {
        if ("userKey".equals(key) || "key".equals(key)) {
            return userKey;
        }
        return attributes.get(key);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static EvaluationContext ofUser(String userKey) {
        return builder().userKey(userKey).build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvaluationContext that = (EvaluationContext) o;
        return Objects.equals(userKey, that.userKey) && Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userKey, attributes);
    }

    @Override
    public String toString() {
        return "EvaluationContext{" +
                "userKey='" + userKey + '\'' +
                ", attributes=" + attributes +
                '}';
    }

    /**
     * Builder for {@link EvaluationContext}.
     */
    public static final class Builder {
        private String userKey = "anonymous";
        private final Map<String, Object> attributes = new HashMap<>();

        private Builder() {}

        public Builder userKey(String userKey) {
            this.userKey = userKey != null ? userKey : "anonymous";
            return this;
        }

        public Builder attribute(String key, Object value) {
            if (key != null && value != null) {
                this.attributes.put(key, value);
            }
            return this;
        }

        public Builder attributes(Map<String, Object> attributes) {
            if (attributes != null) {
                this.attributes.putAll(attributes);
            }
            return this;
        }

        public EvaluationContext build() {
            return new EvaluationContext(userKey, attributes);
        }
    }
}
