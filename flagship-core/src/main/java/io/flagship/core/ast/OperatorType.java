package io.flagship.core.ast;

/**
 * Strategy operators supported for rule evaluation (FR2).
 */
public enum OperatorType {
    EQUALS,
    NOT_EQUALS,
    IN,
    NOT_IN,
    CONTAINS,
    NOT_CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    SEMVER_EQ,
    SEMVER_GT,
    SEMVER_GTE,
    SEMVER_LT,
    SEMVER_LTE,
    REGEX,
    DATE_BEFORE,
    DATE_AFTER
}
