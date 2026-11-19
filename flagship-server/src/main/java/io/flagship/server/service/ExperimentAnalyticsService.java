package io.flagship.server.service;

import io.flagship.server.domain.ExperimentEvent;
import io.flagship.server.domain.Flag;
import io.flagship.server.repository.ExperimentEventRepository;
import io.flagship.server.repository.FlagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Computes Experiment Analytics, Chi-square SRM checks, and Two-proportion Z-tests (FR8).
 */
@Service
public class ExperimentAnalyticsService {

    private final ExperimentEventRepository experimentEventRepository;
    private final FlagRepository flagRepository;

    public ExperimentAnalyticsService(
            ExperimentEventRepository experimentEventRepository,
            FlagRepository flagRepository
    ) {
        this.experimentEventRepository = experimentEventRepository;
        this.flagRepository = flagRepository;
    }

    public record VariationStats(
            String variationId,
            long exposures,
            long conversions,
            double conversionRate,
            Double zScore,
            Double pValue,
            double[] confidenceInterval95,
            boolean statisticallySignificant,
            String status
    ) {}

    public record ExperimentReport(
            String flagKey,
            String baselineVariationId,
            Map<String, VariationStats> variationStats,
            double srmChiSquare,
            double srmPValue,
            boolean srmAlert,
            String summary
    ) {}

    @Transactional
    public void recordEventsBatch(List<ExperimentEvent> events) {
        if (events == null || events.isEmpty()) return;

        for (ExperimentEvent event : events) {
            if (event.getId() == null) {
                event.setId("evt-" + UUID.randomUUID());
            }
            if (event.getTimestamp() == null) {
                event.setTimestamp(Instant.now());
            }
        }
        experimentEventRepository.saveAll(events);

        // Update lastEvaluatedAt for hygiene tracking
        Set<String> flagKeys = new HashSet<>();
        for (ExperimentEvent e : events) {
            flagKeys.add(e.getFlagKey());
        }
        for (String flagKey : flagKeys) {
            flagRepository.findAll().stream()
                    .filter(f -> f.getFlagKey().equals(flagKey))
                    .findFirst()
                    .ifPresent(flag -> {
                        flag.setLastEvaluatedAt(Instant.now());
                        flagRepository.save(flag);
                    });
        }
    }

    @Transactional(readOnly = true)
    public ExperimentReport analyzeExperiment(
            String projectId,
            String envKey,
            String flagKey,
            Instant since,
            String baselineVariationId
    ) {
        Instant sinceTime = (since != null) ? since : Instant.EPOCH;
        List<ExperimentEvent> events = experimentEventRepository.findEventsForAnalysis(projectId, envKey, flagKey, sinceTime);

        // Group exposures by userKey and variationId
        Map<String, Set<String>> variationUsers = new HashMap<>();
        Map<String, Set<String>> variationConvertedUsers = new HashMap<>();

        for (ExperimentEvent e : events) {
            String varId = e.getVariationId();
            if (e.getEventType() == ExperimentEvent.EventType.EXPOSURE) {
                variationUsers.computeIfAbsent(varId, k -> new HashSet<>()).add(e.getUserKey());
            } else if (e.getEventType() == ExperimentEvent.EventType.GOAL) {
                variationConvertedUsers.computeIfAbsent(varId, k -> new HashSet<>()).add(e.getUserKey());
            }
        }

        // 1. Compute exposures and conversions
        Map<String, Long> exposures = new HashMap<>();
        Map<String, Long> conversions = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : variationUsers.entrySet()) {
            String varId = entry.getKey();
            Set<String> exposed = entry.getValue();
            exposures.put(varId, (long) exposed.size());

            Set<String> converted = variationConvertedUsers.getOrDefault(varId, Collections.emptySet());
            // Count unique users who were exposed and also converted
            long convCount = converted.stream().filter(exposed::contains).count();
            conversions.put(varId, convCount);
        }

        if (exposures.isEmpty()) {
            return new ExperimentReport(flagKey, baselineVariationId, Map.of(), 0.0, 1.0, false, "No exposure data recorded yet");
        }

        // Determine baseline variation
        String effectiveBaseline = baselineVariationId;
        if (effectiveBaseline == null || !exposures.containsKey(effectiveBaseline)) {
            effectiveBaseline = exposures.keySet().iterator().next();
        }

        long baselineExp = exposures.getOrDefault(effectiveBaseline, 0L);
        long baselineConv = conversions.getOrDefault(effectiveBaseline, 0L);
        double baselineRate = baselineExp > 0 ? (double) baselineConv / baselineExp : 0.0;

        // 2. Perform Chi-square Sample-Ratio Mismatch (SRM) Test
        int numVariations = exposures.size();
        long totalExposures = exposures.values().stream().mapToLong(Long::longValue).sum();
        double expectedPerVariation = (double) totalExposures / numVariations;

        double srmChiSquare = 0.0;
        if (expectedPerVariation > 0 && numVariations > 1) {
            for (long observed : exposures.values()) {
                double diff = observed - expectedPerVariation;
                srmChiSquare += (diff * diff) / expectedPerVariation;
            }
        }

        int df = Math.max(1, numVariations - 1);
        double srmPValue = computeChiSquarePValue(srmChiSquare, df);
        boolean srmAlert = (srmPValue < 0.01 && totalExposures >= 30);

        // 3. Perform Two-Proportion Z-Test for each variation against baseline
        Map<String, VariationStats> statsMap = new HashMap<>();

        for (Map.Entry<String, Long> entry : exposures.entrySet()) {
            String varId = entry.getKey();
            long varExp = entry.getValue();
            long varConv = conversions.getOrDefault(varId, 0L);
            double varRate = varExp > 0 ? (double) varConv / varExp : 0.0;

            if (varId.equals(effectiveBaseline)) {
                statsMap.put(varId, new VariationStats(
                        varId, varExp, varConv, varRate, 0.0, 1.0,
                        new double[]{varRate, varRate}, false, "BASELINE"
                ));
            } else if (varExp < 10 || baselineExp < 10) {
                statsMap.put(varId, new VariationStats(
                        varId, varExp, varConv, varRate, null, null,
                        new double[]{varRate, varRate}, false, "INSUFFICIENT_DATA"
                ));
            } else {
                // Two-proportion Z-test
                double pooledP = (double) (varConv + baselineConv) / (varExp + baselineExp);
                double pooledVariance = pooledP * (1.0 - pooledP) * ((1.0 / varExp) + (1.0 / baselineExp));

                if (pooledVariance <= 0) {
                    statsMap.put(varId, new VariationStats(
                            varId, varExp, varConv, varRate, 0.0, 1.0,
                            new double[]{varRate, varRate}, false, "ZERO_VARIANCE"
                    ));
                } else {
                    double se = Math.sqrt(pooledVariance);
                    double z = (varRate - baselineRate) / se;
                    double pVal = 2.0 * (1.0 - normalCdf(Math.abs(z)));

                    // 95% Confidence Interval for difference (varRate - baselineRate)
                    double seDiff = Math.sqrt((varRate * (1.0 - varRate) / varExp) +
                                              (baselineRate * (1.0 - baselineRate) / baselineExp));
                    double margin = 1.96 * seDiff;
                    double diff = varRate - baselineRate;
                    double[] ci = new double[]{diff - margin, diff + margin};

                    boolean significant = (pVal < 0.05);
                    statsMap.put(varId, new VariationStats(
                            varId, varExp, varConv, varRate, z, pVal, ci, significant,
                            significant ? (z > 0 ? "WINNER" : "LOSER") : "INCONCLUSIVE"
                    ));
                }
            }
        }

        String summary = srmAlert ?
                "Warning: Sample-Ratio Mismatch detected (p = " + String.format("%.4f", srmPValue) + " < 0.01). Traffic may be biased." :
                "Experiment health normal. Baseline: " + effectiveBaseline;

        return new ExperimentReport(
                flagKey, effectiveBaseline, statsMap, srmChiSquare, srmPValue, srmAlert, summary
        );
    }

    /**
     * Standard Normal Cumulative Distribution Function (Abramowitz and Stegun approximation).
     */
    public static double normalCdf(double z) {
        if (z < -8.0) return 0.0;
        if (z > 8.0) return 1.0;

        double b1 = 0.319381530;
        double b2 = -0.356563782;
        double b3 = 1.781477937;
        double b4 = -1.821255978;
        double b5 = 1.330274429;
        double p = 0.2316419;
        double c = 0.39894228;

        double x = Math.abs(z);
        double t = 1.0 / (1.0 + p * x);
        double val = 1.0 - c * Math.exp(-x * x / 2.0) * t *
                (t * (t * (t * (t * b5 + b4) + b3) + b2) + b1);

        return z >= 0 ? val : 1.0 - val;
    }

    /**
     * Chi-square distribution p-value approximation via Wilson-Hilferty transformation.
     */
    public static double computeChiSquarePValue(double chiSquare, int df) {
        if (chiSquare <= 0 || df <= 0) return 1.0;
        // Wilson-Hilferty transformation to standard normal
        double s = 2.0 / (9.0 * df);
        double z = (Math.pow(chiSquare / df, 1.0 / 3.0) - (1.0 - s)) / Math.sqrt(s);
        return 1.0 - normalCdf(z);
    }
}
