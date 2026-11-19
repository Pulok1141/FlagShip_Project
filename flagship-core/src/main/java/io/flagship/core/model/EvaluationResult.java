package io.flagship.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Immutable evaluation result returned by the FlagShip evaluation engine.
 */
public record EvaluationResult(
        @JsonProperty("flagKey") String flagKey,
        @JsonProperty("variationId") String variationId,
        @JsonProperty("variationName") String variationName,
        @JsonProperty("value") Object value,
        @JsonProperty("reason") EvaluationReason reason,
        @JsonProperty("matchedRuleId") String matchedRuleId,
        @JsonProperty("evaluationDurationNanos") long evaluationDurationNanos
) {
    public EvaluationResult {
        Objects.requireNonNull(flagKey, "flagKey must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
    }

    public static EvaluationResult of(String flagKey, Object value, String variationId, EvaluationReason reason) {
        return new EvaluationResult(flagKey, variationId, variationId != null ? variationId : "default", value, reason, null, 0L);
    }

    public static EvaluationResult of(
            String flagKey,
            Variation variation,
            EvaluationReason reason,
            String matchedRuleId,
            long durationNanos
    ) {
        if (variation == null) {
            return new EvaluationResult(flagKey, null, null, null, reason, matchedRuleId, durationNanos);
        }
        return new EvaluationResult(
                flagKey,
                variation.id(),
                variation.name(),
                variation.value(),
                reason,
                matchedRuleId,
                durationNanos
        );
    }

    public static EvaluationResult error(String flagKey, Object defaultValue, String errorMessage) {
        return new EvaluationResult(
                flagKey,
                "error",
                "Error Fallback",
                defaultValue,
                EvaluationReason.ERROR,
                errorMessage,
                0L
        );
    }

    @SuppressWarnings("unchecked")
    public <T> T getValueAs(Class<T> type) {
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        if (type == Boolean.class || type == boolean.class) {
            return (T) Boolean.valueOf(value.toString());
        }
        if (type == String.class) {
            return (T) value.toString();
        }
        if (type == Integer.class || type == int.class) {
            if (value instanceof Number n) {
                return (T) Integer.valueOf(n.intValue());
            }
            return (T) Integer.valueOf(value.toString());
        }
        if (type == Long.class || type == long.class) {
            if (value instanceof Number n) {
                return (T) Long.valueOf(n.longValue());
            }
            return (T) Long.valueOf(value.toString());
        }
        if (type == Double.class || type == double.class) {
            if (value instanceof Number n) {
                return (T) Double.valueOf(n.doubleValue());
            }
            return (T) Double.valueOf(value.toString());
        }
        return (T) value;
    }
}
