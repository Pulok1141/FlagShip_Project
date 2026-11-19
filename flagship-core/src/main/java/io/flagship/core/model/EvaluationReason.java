package io.flagship.core.model;

/**
 * Reason explaining why a specific variation was selected during evaluation.
 * Enforces the strict Chain of Responsibility evaluation order (FR2).
 */
public enum EvaluationReason {
    /** 1. Kill Switch was active (flag disabled). */
    KILL_SWITCH,
    /** 2. Matched an explicit individual user target. */
    TARGET_MATCH,
    /** 3. Matched a segment rule via Composite AST evaluation. */
    RULE_MATCH,
    /** 4. Matched a percentage rollout bucket via MurmurHash3. */
    PERCENTAGE_ROLLOUT,
    /** 5. Fell through to the default variation. */
    DEFAULT,
    /** Evaluation failed or flag was missing; returned caller default. */
    ERROR
}
