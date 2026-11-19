package io.flagship.starter;

import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.model.EvaluationContext;
import io.flagship.core.model.EvaluationReason;
import io.flagship.core.model.EvaluationResult;
import io.flagship.starter.annotation.FeatureFlag;
import io.flagship.starter.annotation.UserKey;
import io.flagship.starter.aop.DefaultEvaluationContextProvider;
import io.flagship.starter.aop.FeatureFlagAspect;
import io.flagship.starter.client.FlagShipClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureFlagAspectTest {

    static class SampleService {

        @FeatureFlag(key = "new-algorithm", fallbackMethod = "legacyAlgorithm")
        public String executeAlgorithm(@UserKey String userId, String query) {
            return "NEW_ALGO: " + query + " for " + userId;
        }

        public String legacyAlgorithm(String userId, String query) {
            return "LEGACY_ALGO: " + query + " for " + userId;
        }

        @FeatureFlag(key = "boolean-feature", defaultValue = "false")
        public boolean isSpecialFeatureEnabled() {
            return true;
        }
    }

    @Test
    @DisplayName("FR4: @FeatureFlag routes to main method when enabled and fallbackMethod when disabled")
    void testFeatureFlagAspectRouting() {
        // FlagShipClient that returns true for user-vip and false for user-standard
        FlagShipClient mockClient = new FlagShipClient() {
            @Override
            public boolean getBoolean(String key, boolean defaultValue) { return false; }
            @Override
            public boolean getBoolean(String key, EvaluationContext ctx, boolean defaultValue) { return false; }
            @Override
            public String getString(String key, String defaultValue) { return defaultValue; }
            @Override
            public String getString(String key, EvaluationContext ctx, String defaultValue) { return defaultValue; }
            @Override
            public double getNumber(String key, double defaultValue) { return defaultValue; }
            @Override
            public double getNumber(String key, EvaluationContext ctx, double defaultValue) { return defaultValue; }
            @Override
            public <T> T getObject(String key, Class<T> targetClass, T defaultValue) { return defaultValue; }
            @Override
            public <T> T getObject(String key, EvaluationContext ctx, Class<T> targetClass, T defaultValue) { return defaultValue; }

            @Override
            public EvaluationResult evaluate(String key, EvaluationContext ctx, Object defaultValue) {
                if ("user-vip".equals(ctx.userKey())) {
                    return EvaluationResult.of(key, true, "v-true", EvaluationReason.TARGET_MATCH);
                }
                return EvaluationResult.of(key, false, "v-false", EvaluationReason.DEFAULT);
            }

            @Override
            public void track(String flagKey, String userKey, String goalName, double value) {}
            @Override
            public CompiledSnapshot getSnapshot() { return CompiledSnapshot.empty(); }
            @Override
            public void updateSnapshot(CompiledSnapshot newSnapshot) {}
        };

        FeatureFlagAspect aspect = new FeatureFlagAspect(mockClient, new DefaultEvaluationContextProvider());

        // Create proxy with aspect
        SampleService target = new SampleService();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        SampleService proxy = factory.getProxy();

        // 1. VIP user gets new algorithm
        String resultVip = proxy.executeAlgorithm("user-vip", "search-flights");
        assertThat(resultVip).isEqualTo("NEW_ALGO: search-flights for user-vip");

        // 2. Standard user falls back to legacy algorithm
        String resultStandard = proxy.executeAlgorithm("user-standard", "search-flights");
        assertThat(resultStandard).isEqualTo("LEGACY_ALGO: search-flights for user-standard");

        // 3. Flag disabled with no fallback returns default value
        boolean boolRes = proxy.isSpecialFeatureEnabled();
        assertThat(boolRes).isFalse();
    }
}
