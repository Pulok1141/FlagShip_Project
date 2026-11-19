package io.flagship.core.visitor;

import io.flagship.core.ast.CompositeRuleNode;
import io.flagship.core.ast.LeafConditionNode;
import io.flagship.core.ast.OperatorType;
import io.flagship.core.ast.RuleNode;
import io.flagship.core.model.EvaluationContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AstVisitorTest {

    @Test
    @DisplayName("Evaluate nested Composite AST (AND/OR trees) via RuleEvaluationVisitor")
    void testCompositeEvaluation() {
        // Condition: (plan == 'enterprise' AND country == 'US') OR (role == 'beta-tester')
        RuleNode node1 = LeafConditionNode.of("plan", OperatorType.EQUALS, "enterprise");
        RuleNode node2 = LeafConditionNode.of("country", OperatorType.EQUALS, "US");
        RuleNode andBranch = CompositeRuleNode.and(node1, node2);

        RuleNode betaTester = LeafConditionNode.of("role", OperatorType.EQUALS, "beta-tester");
        RuleNode root = CompositeRuleNode.or(andBranch, betaTester);

        // Context 1: enterprise + US -> should match
        EvaluationContext ctx1 = EvaluationContext.builder()
                .userKey("user-1")
                .attribute("plan", "enterprise")
                .attribute("country", "US")
                .build();
        assertThat(root.accept(new RuleEvaluationVisitor(ctx1))).isTrue();

        // Context 2: enterprise + UK -> false branch 1, false branch 2 -> false
        EvaluationContext ctx2 = EvaluationContext.builder()
                .userKey("user-2")
                .attribute("plan", "enterprise")
                .attribute("country", "UK")
                .build();
        assertThat(root.accept(new RuleEvaluationVisitor(ctx2))).isFalse();

        // Context 3: free + UK, but beta-tester -> matches branch 2
        EvaluationContext ctx3 = EvaluationContext.builder()
                .userKey("user-3")
                .attribute("plan", "free")
                .attribute("country", "UK")
                .attribute("role", "beta-tester")
                .build();
        assertThat(root.accept(new RuleEvaluationVisitor(ctx3))).isTrue();
    }

    @Test
    @DisplayName("Validate rule AST with RuleValidationVisitor")
    void testAstValidation() {
        RuleValidationVisitor validator = new RuleValidationVisitor();

        // Valid leaf
        RuleNode validLeaf = LeafConditionNode.of("email", OperatorType.CONTAINS, "@acme.com");
        ValidationResult res1 = validLeaf.accept(validator);
        assertThat(res1.isValid()).isTrue();

        // Invalid regex leaf
        RuleNode invalidRegex = LeafConditionNode.of("code", OperatorType.REGEX, "[a-z");
        ValidationResult res2 = invalidRegex.accept(validator);
        assertThat(res2.isValid()).isFalse();
        assertThat(res2.errors().get(0)).contains("Invalid regex syntax");

        // Empty composite node
        RuleNode emptyComposite = CompositeRuleNode.and();
        ValidationResult res3 = emptyComposite.accept(validator);
        assertThat(res3.isValid()).isFalse();
        assertThat(res3.errors().get(0)).contains("must have at least one child");
    }
}
