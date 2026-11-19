package io.flagship.starter.client;

import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.model.EvaluationContext;
import io.flagship.core.model.EvaluationReason;
import io.flagship.core.model.EvaluationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resilient Decorator guaranteeing the Zero-Exception Contract (FR6, QA2).
 * Catches all Throwables and returns the caller-supplied default value under any failure.
 */
public class ResilientFlagShipClient implements FlagShipClient {

    private static final Logger log = LoggerFactory.getLogger(ResilientFlagShipClient.class);

    private final FlagShipClient delegate;

    public ResilientFlagShipClient(FlagShipClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            return delegate.getBoolean(key, defaultValue);
        } catch (Throwable t) {
            log.warn("Resilience caught exception evaluating boolean flag [{}]: {}", key, t.getMessage());
            return defaultValue;
        }
    }

    @Override
    public boolean getBoolean(String key, EvaluationContext ctx, boolean defaultValue) {
        try {
            return delegate.getBoolean(key, ctx, defaultValue);
        } catch (Throwable t) {
            log.warn("Resilience caught exception evaluating boolean flag [{}]: {}", key, t.getMessage());
            return defaultValue;
        }
    }

    @Override
    public String getString(String key, String defaultValue) {
        try {
            return delegate.getString(key, defaultValue);
        } catch (Throwable t) {
            log.warn("Resilience caught exception evaluating string flag [{}]: {}", key, t.getMessage());
            return defaultValue;
        }
    }

    @Override
    public String getString(String key, EvaluationContext ctx, String defaultValue) {
        try {
            return delegate.getString(key, ctx, defaultValue);
        } catch (Throwable t) {
            log.warn("Resilience caught exception evaluating string flag [{}]: {}", key, t.getMessage());
            return defaultValue;
        }
    }

    @Override
    public double getNumber(String key, double defaultValue) {
        try {
            return delegate.getNumber(key, defaultValue);
        } catch (Throwable t) {
            log.warn("Resilience caught exception evaluating number flag [{}]: {}", key, t.getMessage());
            return defaultValue;
        }
    }

    @Override
    public double getNumber(String key, EvaluationContext ctx, double defaultValue) {
        try {
            return delegate.getNumber(key, ctx, defaultValue);
        } catch (Throwable t) {
            log.warn("Resilience caught exception evaluating number flag [{}]: {}", key, t.getMessage());
            return defaultValue;
        }
    }

    @Override
    public <T> T getObject(String key, Class<T> targetClass, T defaultValue) {
        try {
            return delegate.getObject(key, targetClass, defaultValue);
        } catch (Throwable t) {
            log.warn("Resilience caught exception evaluating object flag [{}]: {}", key, t.getMessage());
            return defaultValue;
        }
    }

    @Override
    public <T> T getObject(String key, EvaluationContext ctx, Class<T> targetClass, T defaultValue) {
        try {
            return delegate.getObject(key, ctx, targetClass, defaultValue);
        } catch (Throwable t) {
            log.warn("Resilience caught exception evaluating object flag [{}]: {}", key, t.getMessage());
            return defaultValue;
        }
    }

    @Override
    public EvaluationResult evaluate(String key, EvaluationContext ctx, Object defaultValue) {
        try {
            return delegate.evaluate(key, ctx, defaultValue);
        } catch (Throwable t) {
            log.warn("Resilience caught exception evaluating flag [{}]: {}", key, t.getMessage());
            return EvaluationResult.error(key, defaultValue, t.getMessage());
        }
    }

    @Override
    public void track(String flagKey, String userKey, String goalName, double value) {
        try {
            delegate.track(flagKey, userKey, goalName, value);
        } catch (Throwable t) {
            log.debug("Resilience caught exception tracking goal [{}]: {}", goalName, t.getMessage());
        }
    }

    @Override
    public CompiledSnapshot getSnapshot() {
        try {
            return delegate.getSnapshot();
        } catch (Throwable t) {
            log.warn("Resilience caught exception getting snapshot: {}", t.getMessage());
            return CompiledSnapshot.empty();
        }
    }

    @Override
    public void updateSnapshot(CompiledSnapshot newSnapshot) {
        try {
            delegate.updateSnapshot(newSnapshot);
        } catch (Throwable t) {
            log.warn("Resilience caught exception updating snapshot: {}", t.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            delegate.close();
        } catch (Throwable ignored) {}
    }
}
