package io.flagship.core.strategy;

import io.flagship.core.ast.OperatorType;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Registry of strategy operators for condition evaluation.
 */
public final class OperatorRegistry {

    private final Map<OperatorType, OperatorStrategy> strategies = new EnumMap<>(OperatorType.class);

    private static final OperatorRegistry DEFAULT_INSTANCE = new OperatorRegistry();

    public static OperatorRegistry getDefault() {
        return DEFAULT_INSTANCE;
    }

    public OperatorRegistry() {
        registerDefaultStrategies();
    }

    public OperatorStrategy getStrategy(OperatorType type) {
        OperatorStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported operator type: " + type);
        }
        return strategy;
    }

    public void register(OperatorType type, OperatorStrategy strategy) {
        strategies.put(type, Objects.requireNonNull(strategy, "strategy must not be null"));
    }

    private void registerDefaultStrategies() {
        strategies.put(OperatorType.EQUALS, (attr, targets) -> {
            if (attr == null || targets == null || targets.isEmpty()) return false;
            String attrStr = attr.toString();
            return targets.stream().anyMatch(t -> t != null && t.equalsIgnoreCase(attrStr));
        });

        strategies.put(OperatorType.NOT_EQUALS, (attr, targets) -> {
            if (attr == null) return true;
            if (targets == null || targets.isEmpty()) return true;
            String attrStr = attr.toString();
            return targets.stream().noneMatch(t -> t != null && t.equalsIgnoreCase(attrStr));
        });

        strategies.put(OperatorType.IN, (attr, targets) -> {
            if (attr == null || targets == null || targets.isEmpty()) return false;
            if (attr instanceof Collection<?> coll) {
                Set<String> targetSet = new HashSet<>(targets);
                return coll.stream().anyMatch(item -> item != null && targetSet.contains(item.toString()));
            }
            String attrStr = attr.toString();
            return targets.contains(attrStr);
        });

        strategies.put(OperatorType.NOT_IN, (attr, targets) -> {
            if (attr == null) return true;
            if (targets == null || targets.isEmpty()) return true;
            if (attr instanceof Collection<?> coll) {
                Set<String> targetSet = new HashSet<>(targets);
                return coll.stream().noneMatch(item -> item != null && targetSet.contains(item.toString()));
            }
            return !targets.contains(attr.toString());
        });

        strategies.put(OperatorType.CONTAINS, (attr, targets) -> {
            if (attr == null || targets == null || targets.isEmpty()) return false;
            String attrStr = attr.toString();
            return targets.stream().anyMatch(t -> t != null && attrStr.contains(t));
        });

        strategies.put(OperatorType.NOT_CONTAINS, (attr, targets) -> {
            if (attr == null) return true;
            if (targets == null || targets.isEmpty()) return true;
            String attrStr = attr.toString();
            return targets.stream().noneMatch(t -> t != null && attrStr.contains(t));
        });

        strategies.put(OperatorType.STARTS_WITH, (attr, targets) -> {
            if (attr == null || targets == null || targets.isEmpty()) return false;
            String attrStr = attr.toString();
            return targets.stream().anyMatch(t -> t != null && attrStr.startsWith(t));
        });

        strategies.put(OperatorType.ENDS_WITH, (attr, targets) -> {
            if (attr == null || targets == null || targets.isEmpty()) return false;
            String attrStr = attr.toString();
            return targets.stream().anyMatch(t -> t != null && attrStr.endsWith(t));
        });

        strategies.put(OperatorType.REGEX, (attr, targets) -> {
            if (attr == null || targets == null || targets.isEmpty()) return false;
            String attrStr = attr.toString();
            for (String regex : targets) {
                if (regex != null) {
                    try {
                        if (Pattern.compile(regex).matcher(attrStr).find()) {
                            return true;
                        }
                    } catch (Exception ignored) {}
                }
            }
            return false;
        });

        // SemVer Strategies
        strategies.put(OperatorType.SEMVER_EQ, (attr, targets) -> compareSemVer(attr, targets, 0, true));
        strategies.put(OperatorType.SEMVER_GT, (attr, targets) -> compareSemVer(attr, targets, 1, false));
        strategies.put(OperatorType.SEMVER_GTE, (attr, targets) -> compareSemVer(attr, targets, 0, true) || compareSemVer(attr, targets, 1, false));
        strategies.put(OperatorType.SEMVER_LT, (attr, targets) -> compareSemVer(attr, targets, -1, false));
        strategies.put(OperatorType.SEMVER_LTE, (attr, targets) -> compareSemVer(attr, targets, 0, true) || compareSemVer(attr, targets, -1, false));

        // Date Strategies (ISO-8601 or Epoch ms)
        strategies.put(OperatorType.DATE_BEFORE, (attr, targets) -> {
            Instant attrDate = parseInstant(attr);
            if (attrDate == null || targets == null || targets.isEmpty()) return false;
            for (String target : targets) {
                Instant targetDate = parseInstant(target);
                if (targetDate != null && attrDate.isBefore(targetDate)) {
                    return true;
                }
            }
            return false;
        });

        strategies.put(OperatorType.DATE_AFTER, (attr, targets) -> {
            Instant attrDate = parseInstant(attr);
            if (attrDate == null || targets == null || targets.isEmpty()) return false;
            for (String target : targets) {
                Instant targetDate = parseInstant(target);
                if (targetDate != null && attrDate.isAfter(targetDate)) {
                    return true;
                }
            }
            return false;
        });
    }

    private static boolean compareSemVer(Object attr, List<String> targets, int expectedSign, boolean exact) {
        if (attr == null || targets == null || targets.isEmpty()) return false;
        SemVer attrVer = SemVer.parseOrNull(attr.toString());
        if (attrVer == null) return false;

        for (String target : targets) {
            SemVer targetVer = SemVer.parseOrNull(target);
            if (targetVer != null) {
                int cmp = attrVer.compareTo(targetVer);
                if (exact) {
                    if (cmp == 0) return true;
                } else {
                    if (expectedSign > 0 && cmp > 0) return true;
                    if (expectedSign < 0 && cmp < 0) return true;
                }
            }
        }
        return false;
    }

    public static Instant parseInstant(Object value) {
        if (value == null) return null;
        if (value instanceof Instant inst) return inst;
        if (value instanceof Number num) {
            return Instant.ofEpochMilli(num.longValue());
        }
        String str = value.toString().trim();
        try {
            return Instant.parse(str);
        } catch (DateTimeParseException ignored) {
            try {
                long epoch = Long.parseLong(str);
                return Instant.ofEpochMilli(epoch);
            } catch (NumberFormatException ignored2) {
                return null;
            }
        }
    }
}
