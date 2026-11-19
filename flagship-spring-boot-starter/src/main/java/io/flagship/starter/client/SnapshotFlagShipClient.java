package io.flagship.starter.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.engine.DefaultEvaluationEngine;
import io.flagship.core.engine.EvaluationEngine;
import io.flagship.core.model.EvaluationContext;
import io.flagship.core.model.EvaluationResult;
import io.flagship.core.model.SnapshotJsonMapper;
import io.flagship.starter.sync.AsyncEventBatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * In-Memory Snapshot Client for sub-50µs lock-free evaluation (QA1, QA4).
 * Swaps CompiledSnapshot instances atomically via AtomicReference.
 */
public class SnapshotFlagShipClient implements FlagShipClient {

    private static final Logger log = LoggerFactory.getLogger(SnapshotFlagShipClient.class);

    private final AtomicReference<CompiledSnapshot> snapshotRef;
    private final EvaluationEngine engine;
    private final AsyncEventBatcher eventBatcher;
    private final ObjectMapper mapper = SnapshotJsonMapper.getMapper();

    public SnapshotFlagShipClient(CompiledSnapshot initialSnapshot, AsyncEventBatcher eventBatcher) {
        this.snapshotRef = new AtomicReference<>(initialSnapshot != null ? initialSnapshot : CompiledSnapshot.empty());
        this.engine = new DefaultEvaluationEngine();
        this.eventBatcher = eventBatcher;
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return getBoolean(key, EvaluationContext.anonymous(), defaultValue);
    }

    @Override
    public boolean getBoolean(String key, EvaluationContext ctx, boolean defaultValue) {
        EvaluationResult result = evaluate(key, ctx, defaultValue);
        Object val = result.value();
        if (val instanceof Boolean b) {
            return b;
        }
        if (val instanceof String s) {
            return Boolean.parseBoolean(s);
        }
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
        if (val instanceof Number n) {
            return n.doubleValue();
        }
        if (val != null) {
            try {
                return Double.parseDouble(val.toString());
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    @Override
    public <T> T getObject(String key, Class<T> targetClass, T defaultValue) {
        return getObject(key, EvaluationContext.anonymous(), targetClass, defaultValue);
    }

    @Override
    public <T> T getObject(String key, EvaluationContext ctx, Class<T> targetClass, T defaultValue) {
        EvaluationResult result = evaluate(key, ctx, defaultValue);
        Object val = result.value();
        if (val == null) return defaultValue;
        if (targetClass.isInstance(val)) {
            return targetClass.cast(val);
        }
        try {
            if (val instanceof String s) {
                return mapper.readValue(s, targetClass);
            }
            return mapper.convertValue(val, targetClass);
        } catch (Exception e) {
            log.debug("Failed to deserialize flag [{}] object to {}: {}", key, targetClass.getSimpleName(), e.getMessage());
            return defaultValue;
        }
    }

    @Override
    public EvaluationResult evaluate(String key, EvaluationContext ctx, Object defaultValue) {
        EvaluationContext context = (ctx != null) ? ctx : EvaluationContext.anonymous();
        CompiledSnapshot snapshot = snapshotRef.get();

        EvaluationResult result = engine.evaluate(key, context, snapshot);

        // Record exposure event asynchronously
        if (eventBatcher != null && result.variationId() != null) {
            eventBatcher.recordExposure(key, result.variationId(), context.userKey());
        }

        return result;
    }

    @Override
    public void track(String flagKey, String userKey, String goalName, double value) {
        if (eventBatcher != null) {
            eventBatcher.recordGoal(flagKey, userKey, goalName, value);
        }
    }

    @Override
    public CompiledSnapshot getSnapshot() {
        return snapshotRef.get();
    }

    @Override
    public void updateSnapshot(CompiledSnapshot newSnapshot) {
        if (newSnapshot != null) {
            this.snapshotRef.set(newSnapshot);
            log.debug("Updated CompiledSnapshot with version {}", newSnapshot.version());
        }
    }

    @Override
    public void close() {
        if (eventBatcher != null) {
            eventBatcher.close();
        }
    }
}
