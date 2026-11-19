package io.flagship.core.strategy;

import io.flagship.core.ast.OperatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OperatorStrategyTest {

    private final OperatorRegistry registry = OperatorRegistry.getDefault();

    @Test
    @DisplayName("Test EQUALS and NOT_EQUALS operators")
    void testEqualsOperators() {
        OperatorStrategy equals = registry.getStrategy(OperatorType.EQUALS);
        OperatorStrategy notEquals = registry.getStrategy(OperatorType.NOT_EQUALS);

        assertThat(equals.evaluate("premium", List.of("premium", "enterprise"))).isTrue();
        assertThat(equals.evaluate("free", List.of("premium", "enterprise"))).isFalse();
        assertThat(equals.evaluate(null, List.of("premium"))).isFalse();

        assertThat(notEquals.evaluate("free", List.of("premium", "enterprise"))).isTrue();
        assertThat(notEquals.evaluate("premium", List.of("premium", "enterprise"))).isFalse();
    }

    @Test
    @DisplayName("Test IN and NOT_IN operators for scalars and collections")
    void testInOperators() {
        OperatorStrategy in = registry.getStrategy(OperatorType.IN);
        OperatorStrategy notIn = registry.getStrategy(OperatorType.NOT_IN);

        assertThat(in.evaluate("US", List.of("US", "CA", "UK"))).isTrue();
        assertThat(in.evaluate("DE", List.of("US", "CA", "UK"))).isFalse();
        assertThat(in.evaluate(List.of("ADMIN", "USER"), List.of("ADMIN"))).isTrue();
        assertThat(in.evaluate(List.of("GUEST"), List.of("ADMIN"))).isFalse();

        assertThat(notIn.evaluate("DE", List.of("US", "CA"))).isTrue();
        assertThat(notIn.evaluate("US", List.of("US", "CA"))).isFalse();
    }

    @Test
    @DisplayName("Test CONTAINS, STARTS_WITH, and ENDS_WITH operators")
    void testStringOperators() {
        OperatorStrategy contains = registry.getStrategy(OperatorType.CONTAINS);
        OperatorStrategy startsWith = registry.getStrategy(OperatorType.STARTS_WITH);
        OperatorStrategy endsWith = registry.getStrategy(OperatorType.ENDS_WITH);

        assertThat(contains.evaluate("alice@company.com", List.of("@company.com"))).isTrue();
        assertThat(contains.evaluate("alice@gmail.com", List.of("@company.com"))).isFalse();

        assertThat(startsWith.evaluate("prod-east-1", List.of("prod-"))).isTrue();
        assertThat(startsWith.evaluate("dev-east-1", List.of("prod-"))).isFalse();

        assertThat(endsWith.evaluate("image.png", List.of(".png", ".jpg"))).isTrue();
        assertThat(endsWith.evaluate("image.gif", List.of(".png", ".jpg"))).isFalse();
    }

    @Test
    @DisplayName("Test REGEX operator")
    void testRegexOperator() {
        OperatorStrategy regex = registry.getStrategy(OperatorType.REGEX);

        assertThat(regex.evaluate("order-12345", List.of("^order-\\d+$"))).isTrue();
        assertThat(regex.evaluate("order-abc", List.of("^order-\\d+$"))).isFalse();
    }

    @Test
    @DisplayName("Test SemVer operators")
    void testSemVerOperators() {
        OperatorStrategy eq = registry.getStrategy(OperatorType.SEMVER_EQ);
        OperatorStrategy gt = registry.getStrategy(OperatorType.SEMVER_GT);
        OperatorStrategy gte = registry.getStrategy(OperatorType.SEMVER_GTE);
        OperatorStrategy lt = registry.getStrategy(OperatorType.SEMVER_LT);
        OperatorStrategy lte = registry.getStrategy(OperatorType.SEMVER_LTE);

        assertThat(eq.evaluate("1.2.3", List.of("v1.2.3"))).isTrue();
        assertThat(gt.evaluate("2.0.0", List.of("1.9.9"))).isTrue();
        assertThat(gt.evaluate("1.9.9", List.of("2.0.0"))).isFalse();
        assertThat(gte.evaluate("2.0.0", List.of("2.0.0"))).isTrue();
        assertThat(lt.evaluate("1.0.0-alpha", List.of("1.0.0"))).isTrue();
        assertThat(lte.evaluate("1.0.0", List.of("1.0.0"))).isTrue();
    }

    @Test
    @DisplayName("Test DATE_BEFORE and DATE_AFTER operators")
    void testDateOperators() {
        OperatorStrategy before = registry.getStrategy(OperatorType.DATE_BEFORE);
        OperatorStrategy after = registry.getStrategy(OperatorType.DATE_AFTER);

        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-06-01T00:00:00Z");

        assertThat(before.evaluate(t1, List.of("2026-06-01T00:00:00Z"))).isTrue();
        assertThat(before.evaluate(t2, List.of("2026-01-01T00:00:00Z"))).isFalse();

        assertThat(after.evaluate(t2, List.of("2026-01-01T00:00:00Z"))).isTrue();
        assertThat(after.evaluate(t1, List.of("2026-06-01T00:00:00Z"))).isFalse();
    }
}
