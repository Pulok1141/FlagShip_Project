package io.flagship.core.compiled;

import io.flagship.core.model.FlagDefinition;
import io.flagship.core.model.PercentageRollout;
import io.flagship.core.model.RolloutBucket;
import io.flagship.core.model.SegmentRule;
import io.flagship.core.model.TargetRule;
import io.flagship.core.model.Variation;

import java.util.*;

/**
 * Pre-compiled, immutable flag representation optimized for sub-50µs evaluation.
 * Pre-indexes variation lookups, user target sets, and cumulative rollout thresholds.
 */
public final class CompiledFlag {

    public record CumulativeBucket(int threshold, Variation variation) {}

    private final FlagDefinition definition;
    private final Map<String, Variation> variationMap;
    private final Variation defaultVariation;
    private final Variation offVariation;
    private final List<TargetRule> targets;
    private final List<SegmentRule> rules;
    private final List<CumulativeBucket> cumulativeRollouts;
    private final String rolloutBucketBy;

    public CompiledFlag(FlagDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition must not be null");

        // Pre-index variations for O(1) lookup
        Map<String, Variation> vMap = new HashMap<>();
        for (Variation v : definition.variations()) {
            vMap.put(v.id(), v);
        }
        this.variationMap = Collections.unmodifiableMap(vMap);

        this.defaultVariation = this.variationMap.get(definition.defaultVariationId());
        Variation offVar = definition.offVariationId() != null ? this.variationMap.get(definition.offVariationId()) : null;
        this.offVariation = (offVar != null) ? offVar : this.defaultVariation;

        this.targets = definition.targets() != null ? List.copyOf(definition.targets()) : List.of();
        this.rules = definition.rules() != null ? List.copyOf(definition.rules()) : List.of();

        // Pre-compile cumulative rollout thresholds
        List<CumulativeBucket> cumList = new ArrayList<>();
        PercentageRollout rollout = definition.rollout();
        if (rollout != null && rollout.buckets() != null && !rollout.buckets().isEmpty()) {
            int runningSum = 0;
            for (RolloutBucket b : rollout.buckets()) {
                runningSum += b.weight();
                Variation var = this.variationMap.get(b.variationId());
                if (var != null && b.weight() > 0) {
                    cumList.add(new CumulativeBucket(Math.min(runningSum, 10000), var));
                }
            }
            this.rolloutBucketBy = rollout.bucketBy();
        } else {
            this.rolloutBucketBy = "userKey";
        }
        this.cumulativeRollouts = Collections.unmodifiableList(cumList);
    }

    public FlagDefinition definition() {
        return definition;
    }

    public String key() {
        return definition.key();
    }

    public boolean isEnabled() {
        return definition.enabled();
    }

    public Variation findVariation(String variationId) {
        return variationMap.get(variationId);
    }

    public Variation defaultVariation() {
        return defaultVariation;
    }

    public Variation offVariation() {
        return offVariation;
    }

    public List<TargetRule> targets() {
        return targets;
    }

    public List<SegmentRule> rules() {
        return rules;
    }

    public List<CumulativeBucket> cumulativeRollouts() {
        return cumulativeRollouts;
    }

    public String rolloutBucketBy() {
        return rolloutBucketBy;
    }
}
