package io.flagship.server.service;

import io.flagship.server.domain.ExperimentEvent;
import io.flagship.server.repository.ExperimentEventRepository;
import io.flagship.server.repository.FlagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExperimentAnalyticsTest {

    @Test
    @DisplayName("FR8: Compute conversion rates and Two-proportion Z-test statistical significance")
    void testStatisticalSignificance() {
        List<ExperimentEvent> events = new ArrayList<>();

        // Variation A (Baseline): 1,000 exposures, 100 conversions (10% rate)
        for (int i = 0; i < 1000; i++) {
            events.add(new ExperimentEvent("e-" + i, "p1", "prod", "checkout-flag", "var-a", "user-a-" + i,
                    ExperimentEvent.EventType.EXPOSURE, null, 1.0, Instant.now()));
            if (i < 100) {
                events.add(new ExperimentEvent("c-" + i, "p1", "prod", "checkout-flag", "var-a", "user-a-" + i,
                        ExperimentEvent.EventType.GOAL, "purchase", 1.0, Instant.now()));
            }
        }

        // Variation B (Winner): 1,000 exposures, 150 conversions (15% rate)
        for (int i = 0; i < 1000; i++) {
            events.add(new ExperimentEvent("e-b-" + i, "p1", "prod", "checkout-flag", "var-b", "user-b-" + i,
                    ExperimentEvent.EventType.EXPOSURE, null, 1.0, Instant.now()));
            if (i < 150) {
                events.add(new ExperimentEvent("c-b-" + i, "p1", "prod", "checkout-flag", "var-b", "user-b-" + i,
                        ExperimentEvent.EventType.GOAL, "purchase", 1.0, Instant.now()));
            }
        }

        ExperimentEventRepository eventRepo = (ExperimentEventRepository) Proxy.newProxyInstance(
                ExperimentEventRepository.class.getClassLoader(),
                new Class<?>[]{ExperimentEventRepository.class},
                (proxy, method, args) -> {
                    if ("findEventsForAnalysis".equals(method.getName())) {
                        return events;
                    }
                    return null;
                }
        );

        FlagRepository flagRepo = (FlagRepository) Proxy.newProxyInstance(
                FlagRepository.class.getClassLoader(),
                new Class<?>[]{FlagRepository.class},
                (proxy, method, args) -> null
        );

        ExperimentAnalyticsService service = new ExperimentAnalyticsService(eventRepo, flagRepo);

        ExperimentAnalyticsService.ExperimentReport report =
                service.analyzeExperiment("p1", "prod", "checkout-flag", Instant.EPOCH, "var-a");

        assertThat(report.srmAlert()).isFalse(); // Equal 1000:1000 allocation, Chi-square p-val ~ 1.0
        assertThat(report.variationStats()).hasSize(2);

        var baseline = report.variationStats().get("var-a");
        var treatment = report.variationStats().get("var-b");

        assertThat(baseline.conversions()).isEqualTo(100);
        assertThat(baseline.conversionRate()).isEqualTo(0.10);

        assertThat(treatment.conversions()).isEqualTo(150);
        assertThat(treatment.conversionRate()).isEqualTo(0.15);
        assertThat(treatment.pValue()).isLessThan(0.01); // Statistically highly significant
        assertThat(treatment.statisticallySignificant()).isTrue();
    }

    @Test
    @DisplayName("FR8: Detect Sample Ratio Mismatch (SRM) when exposures deviate significantly from expected")
    void testSampleRatioMismatchDetection() {
        List<ExperimentEvent> events = new ArrayList<>();

        // Variation A: 1,000 exposures
        for (int i = 0; i < 1000; i++) {
            events.add(new ExperimentEvent("e-a-" + i, "p1", "prod", "test-flag", "var-a", "user-a-" + i,
                    ExperimentEvent.EventType.EXPOSURE, null, 1.0, Instant.now()));
        }
        // Variation B: only 700 exposures (severe deviation from expected 50/50 split -> Chi-sq ~ 52.9)
        for (int i = 0; i < 700; i++) {
            events.add(new ExperimentEvent("e-b-" + i, "p1", "prod", "test-flag", "var-b", "user-b-" + i,
                    ExperimentEvent.EventType.EXPOSURE, null, 1.0, Instant.now()));
        }

        ExperimentEventRepository eventRepo = (ExperimentEventRepository) Proxy.newProxyInstance(
                ExperimentEventRepository.class.getClassLoader(),
                new Class<?>[]{ExperimentEventRepository.class},
                (proxy, method, args) -> {
                    if ("findEventsForAnalysis".equals(method.getName())) {
                        return events;
                    }
                    return null;
                }
        );

        FlagRepository flagRepo = (FlagRepository) Proxy.newProxyInstance(
                FlagRepository.class.getClassLoader(),
                new Class<?>[]{FlagRepository.class},
                (proxy, method, args) -> null
        );

        ExperimentAnalyticsService service = new ExperimentAnalyticsService(eventRepo, flagRepo);

        ExperimentAnalyticsService.ExperimentReport report =
                service.analyzeExperiment("p1", "prod", "test-flag", Instant.EPOCH, "var-a");

        assertThat(report.srmAlert()).isTrue();
        assertThat(report.srmPValue()).isLessThan(0.001);
    }
}
