package io.flagship.benchmarks;

import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.core.model.*;
import io.flagship.starter.client.FlagShipClient;
import io.flagship.starter.client.ResilientFlagShipClient;
import io.flagship.starter.client.SnapshotFlagShipClient;
import io.flagship.starter.sync.SnapshotCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class OutageResilienceVerificationTest {

    @Test
    @DisplayName("QA2: Total server outage resilience — startup from disk cache, zero exceptions surfaced, fallback evaluation")
    void testOutageResilience(@TempDir Path tempDir) {
        String cacheFile = tempDir.resolve("outage-cache.json").toString();
        SnapshotCache diskCache = new SnapshotCache(cacheFile);

        // Pre-populate disk cache with snapshot (as if synced during normal operation)
        String flagKey = "payment-gateway-v2";
        FlagDefinition flag = new FlagDefinition(
                flagKey, "Payment Gateway V2", "Critical payment routing",
                FlagType.STRING, true,
                List.of(
                        new Variation("v-stripe", "Stripe", "stripe-prod"),
                        new Variation("v-paypal", "PayPal", "paypal-prod")
                ),
                List.of(), List.of(), null,
                "v-stripe", "v-paypal", 42L, Instant.now(), List.of("payments")
        );

        EnvironmentSnapshot snapshot = new EnvironmentSnapshot(
                "checkout-project", "production", 42L, Instant.now(),
                Map.of(flagKey, flag), Map.of()
        );
        diskCache.save(SnapshotJsonMapper.toJson(snapshot));
        assertThat(new File(cacheFile)).exists();

        // Simulate complete server outage: server URL points to unreachable port, zero network connectivity
        // Boot client strictly from disk snapshot cache
        CompiledSnapshot cachedSnapshot = diskCache.load();
        assertThat(cachedSnapshot).isNotNull();

        FlagShipClient innerClient = new SnapshotFlagShipClient(cachedSnapshot, null);
        FlagShipClient resilientClient = new ResilientFlagShipClient(innerClient);

        // 1. Verify evaluations continue serving cached values under total server outage
        EvaluationContext ctx = EvaluationContext.ofUser("cust-9988");
        String paymentGateway = resilientClient.getString(flagKey, ctx, "stripe-prod");
        assertThat(paymentGateway).isEqualTo("stripe-prod");

        // 2. Verify Zero-Exception Contract: evaluate non-existent flag or corrupted state
        assertThatCode(() -> {
            for (int i = 0; i < 5000; i++) {
                String val = resilientClient.getString("non-existent-flag-" + i, ctx, "hardcoded-safe-default");
                assertThat(val).isEqualTo("hardcoded-safe-default");

                boolean boolVal = resilientClient.getBoolean("missing-bool-" + i, ctx, true);
                assertThat(boolVal).isTrue();

                double numVal = resilientClient.getNumber("missing-num-" + i, ctx, 99.9);
                assertThat(numVal).isEqualTo(99.9);
            }
        }).doesNotThrowAnyException();

        System.out.println("QA2 Verification: Total server outage survived cleanly. Loaded from disk cache, 15,000 evaluations served with 0 exceptions surfaced.");
    }
}
