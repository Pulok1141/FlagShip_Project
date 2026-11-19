package io.flagship.starter;

import io.flagship.starter.aop.FeatureFlagAspect;
import io.flagship.starter.client.FlagShipClient;
import io.flagship.starter.config.FlagShipAutoConfiguration;
import io.flagship.starter.endpoint.FlagShipEndpoint;
import io.flagship.starter.sync.AsyncEventBatcher;
import io.flagship.starter.sync.SnapshotCache;
import io.flagship.starter.sync.SyncEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FlagShipAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FlagShipAutoConfiguration.class));

    @Test
    @DisplayName("FR4: FlagShip auto-configuration registers all beans with offline mode")
    void testAutoConfigurationRegistersBeans() {
        runner.withPropertyValues("flagship.offline-mode=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(FlagShipClient.class);
                    assertThat(context).hasSingleBean(SnapshotCache.class);
                    assertThat(context).hasSingleBean(SyncEngine.class);
                    assertThat(context).hasSingleBean(AsyncEventBatcher.class);
                    assertThat(context).hasSingleBean(FeatureFlagAspect.class);
                    assertThat(context).hasSingleBean(FlagShipEndpoint.class);
                });
    }

    @Test
    @DisplayName("FR4: FlagShip auto-configuration backs off when flagship.enabled=false")
    void testAutoConfigurationDisabled() {
        runner.withPropertyValues("flagship.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(FlagShipClient.class);
                    assertThat(context).doesNotHaveBean(FeatureFlagAspect.class);
                    assertThat(context).doesNotHaveBean(FlagShipEndpoint.class);
                });
    }
}
