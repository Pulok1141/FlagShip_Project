package io.flagship.starter.client;

import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.model.EvaluationContext;
import io.flagship.core.model.EvaluationResult;

import java.io.Closeable;

/**
 * Public Developer API for FlagShip Feature Flag evaluations and experiment tracking.
 * Guarantees zero runtime exceptions to host applications (Zero-Exception Contract).
 */
public interface FlagShipClient extends Closeable {

    boolean getBoolean(String key, boolean defaultValue);

    boolean getBoolean(String key, EvaluationContext ctx, boolean defaultValue);

    String getString(String key, String defaultValue);

    String getString(String key, EvaluationContext ctx, String defaultValue);

    double getNumber(String key, double defaultValue);

    double getNumber(String key, EvaluationContext ctx, double defaultValue);

    <T> T getObject(String key, Class<T> targetClass, T defaultValue);

    <T> T getObject(String key, EvaluationContext ctx, Class<T> targetClass, T defaultValue);

    EvaluationResult evaluate(String key, EvaluationContext ctx, Object defaultValue);

    /**
     * Record a conversion goal event for A/B experiment analytics.
     */
    void track(String flagKey, String userKey, String goalName, double value);

    CompiledSnapshot getSnapshot();

    void updateSnapshot(CompiledSnapshot newSnapshot);

    @Override
    default void close() {}
}
