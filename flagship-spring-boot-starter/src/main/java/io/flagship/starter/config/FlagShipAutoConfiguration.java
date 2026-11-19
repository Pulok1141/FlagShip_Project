package io.flagship.starter.config;

import io.flagship.core.compiled.CompiledSnapshot;
import io.flagship.starter.aop.DefaultEvaluationContextProvider;
import io.flagship.starter.aop.EvaluationContextProvider;
import io.flagship.starter.aop.FeatureFlagAspect;
import io.flagship.starter.client.FlagShipClient;
import io.flagship.starter.client.MeteredFlagShipClient;
import io.flagship.starter.client.ResilientFlagShipClient;
import io.flagship.starter.client.SnapshotFlagShipClient;
import io.flagship.starter.endpoint.FlagShipEndpoint;
import io.flagship.starter.sync.AsyncEventBatcher;
import io.flagship.starter.sync.SnapshotCache;
import io.flagship.starter.sync.SyncEngine;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(FlagShipProperties.class)
@ConditionalOnProperty(prefix = "flagship", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FlagShipAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SnapshotCache snapshotCache(FlagShipProperties props) {
        return new SnapshotCache(props.getCacheFilePath());
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public AsyncEventBatcher asyncEventBatcher(FlagShipProperties props) {
        return new AsyncEventBatcher(
                props.getServerUrl(),
                props.getApiKey(),
                props.getEventFlushIntervalMs(),
                props.getEventBatchSize(),
                10000
        );
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public SyncEngine syncEngine(FlagShipProperties props, SnapshotCache cache) {
        return new SyncEngine(props, cache);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public FlagShipClient flagShipClient(
            FlagShipProperties props,
            SnapshotCache cache,
            AsyncEventBatcher eventBatcher,
            SyncEngine syncEngine,
            ObjectProvider<MeterRegistry> meterRegistryProvider
    ) {
        // 1. Synchronously bootstrap snapshot (capped at <= 2000 ms, with disk cache fallback)
        CompiledSnapshot initialSnapshot = syncEngine.bootstrap();

        // 2. Build Decorator Stack: Metered -> Resilient -> Snapshot
        SnapshotFlagShipClient innerClient = new SnapshotFlagShipClient(initialSnapshot, eventBatcher);
        syncEngine.setClient(innerClient);
        syncEngine.startBackgroundSync();

        ResilientFlagShipClient resilientClient = new ResilientFlagShipClient(innerClient);
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        return new MeteredFlagShipClient(resilientClient, meterRegistry);
    }

    @Bean
    @ConditionalOnClass(name = "org.aspectj.lang.ProceedingJoinPoint")
    @ConditionalOnMissingBean
    public EvaluationContextProvider evaluationContextProvider() {
        return new DefaultEvaluationContextProvider();
    }

    @Bean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    @ConditionalOnMissingBean
    public FeatureFlagAspect featureFlagAspect(FlagShipClient client, EvaluationContextProvider contextProvider) {
        return new FeatureFlagAspect(client, contextProvider);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    @ConditionalOnMissingBean
    public FlagShipEndpoint flagShipEndpoint(FlagShipProperties props, FlagShipClient client, SyncEngine syncEngine) {
        return new FlagShipEndpoint(props, client, syncEngine);
    }
}
