package io.flagship.starter.client;

import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.model.EvaluationContext;
import io.flagship.core.model.EvaluationResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;

/**
 * Observability Decorator instrumenting Flag evaluations with Micrometer metrics (FR4).
 * Emits 'flagship.evaluations.total' counter and 'flagship.evaluation.duration' timer.
 */
public class MeteredFlagShipClient implements FlagShipClient {

    private final FlagShipClient delegate;
    private final MeterRegistry meterRegistry;

    public MeteredFlagShipClient(FlagShipClient delegate, MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return getBoolean(key, EvaluationContext.anonymous(), defaultValue);
    }

    @Override
    public boolean getBoolean(String key, EvaluationContext ctx, boolean defaultValue) {
        EvaluationResult result = evaluate(key, ctx, defaultValue);
        Object val = result.value();
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) return Boolean.parseBoolean(s);
        return defaultValue;
    }

    @Override
    public String getString(String key, String defaultValue) {
        return getString(key, EvaluationContext.anonymous(), defaultValue);
    }

    @Override
    public String getString(String key, EvaluationContext ctx, String defaultValue) {
        EvaluationResult result = evaluate(key, ctx, defaultValue);
        Object val = result.value();
        return val != null ? val.toString() : defaultValue;
    }

    @Override
    public double getNumber(String key, double defaultValue) {
        return getNumber(key, EvaluationContext.anonymous(), defaultValue);
    }

    @Override
    public double getNumber(String key, EvaluationContext ctx, double defaultValue) {
        EvaluationResult result = evaluate(key, ctx, defaultValue);
        Object val = result.value();
        if (val instanceof Number n) return n.doubleValue();
        if (val != null) {
            try {
                return Double.parseDouble(val.toString());
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    @Override
    public <T> T getObject(String key, Class<T> targetClass, T defaultValue) {
        return delegate.getObject(key, targetClass, defaultValue);
    }

    @Override
    public <T> T getObject(String key, EvaluationContext ctx, Class<T> targetClass, T defaultValue) {
        return delegate.getObject(key, ctx, targetClass, defaultValue);
    }

    @Override
    public EvaluationResult evaluate(String key, EvaluationContext ctx, Object defaultValue) {
        if (meterRegistry == null) {
            return delegate.evaluate(key, ctx, defaultValue);
        }

        long start = System.nanoTime();
        EvaluationResult result = delegate.evaluate(key, ctx, defaultValue);
        long durationNanos = System.nanoTime() - start;

        // Record metrics
        try {
            Timer.builder("flagship.evaluation.duration")
                    .description("Time taken to evaluate feature flag in-memory")
                    .tag("flag_key", key)
                    .register(meterRegistry)
                    .record(durationNanos, TimeUnit.NANOSECONDS);

            Counter.builder("flagship.evaluations.total")
                    .description("Total number of feature flag evaluations")
                    .tag("flag_key", key)
                    .tag("variation", result.variationId() != null ? result.variationId() : "none")
                    .tag("reason", result.reason() != null ? result.reason().name() : "UNKNOWN")
                    .register(meterRegistry)
                    .increment();
        } catch (Exception ignored) {}

        return result;
    }

    @Override
    public void track(String flagKey, String userKey, String goalName, double value) {
        delegate.track(flagKey, userKey, goalName, value);
    }

    @Override
    public CompiledSnapshot getSnapshot() {
        return delegate.getSnapshot();
    }

    @Override
    public void updateSnapshot(CompiledSnapshot newSnapshot) {
        delegate.updateSnapshot(newSnapshot);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
