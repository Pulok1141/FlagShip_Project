package io.flagship.starter;

import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.model.EvaluationContext;
import io.flagship.core.model.EvaluationReason;
import io.flagship.core.model.EvaluationResult;
import io.flagship.starter.client.FlagShipClient;
import io.flagship.starter.client.ResilientFlagShipClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class ResilientFlagShipClientTest {

    private final FlagShipClient explosiveClient = new FlagShipClient() {
        @Override
        public boolean getBoolean(String key, boolean defaultValue) {
            throw new RuntimeException("Severe simulated runtime outage!");
        }

        @Override
        public boolean getBoolean(String key, EvaluationContext ctx, boolean defaultValue) {
            throw new RuntimeException("Severe simulated runtime outage!");
        }

        @Override
        public String getString(String key, String defaultValue) {
            throw new RuntimeException("Severe simulated runtime outage!");
        }

        @Override
        public String getString(String key, EvaluationContext ctx, String defaultValue) {
            throw new RuntimeException("Severe simulated runtime outage!");
        }

        @Override
        public double getNumber(String key, double defaultValue) {
            throw new RuntimeException("Severe simulated runtime outage!");
        }

        @Override
        public double getNumber(String key, EvaluationContext ctx, double defaultValue) {
            throw new RuntimeException("Severe simulated runtime outage!");
        }

        @Override
        public <T> T getObject(String key, Class<T> targetClass, T defaultValue) {
            throw new RuntimeException("Severe simulated runtime outage!");
        }

        @Override
        public <T> T getObject(String key, EvaluationContext ctx, Class<T> targetClass, T defaultValue) {
            throw new RuntimeException("Severe simulated runtime outage!");
        }

        @Override
        public EvaluationResult evaluate(String key, EvaluationContext ctx, Object defaultValue) {
            throw new RuntimeException("Severe simulated runtime outage!");
        }

        @Override
        public void track(String flagKey, String userKey, String goalName, double value) {
            throw new RuntimeException("Severe simulated runtime outage!");
        }

        @Override
        public CompiledSnapshot getSnapshot() {
            throw new RuntimeException("Severe simulated runtime outage!");
        }

        @Override
        public void updateSnapshot(CompiledSnapshot newSnapshot) {
            throw new RuntimeException("Severe simulated runtime outage!");
        }
    };

    @Test
    @DisplayName("FR6 / QA2: Zero-Exception Guarantee - Resilient client catches all failures and returns defaults")
    void testZeroExceptionContract() {
        FlagShipClient resilient = new ResilientFlagShipClient(explosiveClient);

        assertThatNoException().isThrownBy(() -> {
            boolean b = resilient.getBoolean("any-key", true);
            assertThat(b).isTrue();

            String s = resilient.getString("any-key", "safe-fallback");
            assertThat(s).isEqualTo("safe-fallback");

            double n = resilient.getNumber("any-key", 42.0);
            assertThat(n).isEqualTo(42.0);

            EvaluationResult res = resilient.evaluate("any-key", EvaluationContext.anonymous(), "default");
            assertThat(res.value()).isEqualTo("default");
            assertThat(res.reason()).isEqualTo(EvaluationReason.ERROR);

            resilient.track("any-key", "user-1", "checkout", 10.0);
            resilient.updateSnapshot(null);
            assertThat(resilient.getSnapshot()).isNotNull();
        });
    }
}
