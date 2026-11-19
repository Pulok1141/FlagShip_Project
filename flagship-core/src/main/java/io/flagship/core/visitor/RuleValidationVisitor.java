package io.flagship.core.visitor;

import io.flagship.core.ast.CompositeRuleNode;
import io.flagship.core.ast.LeafConditionNode;
import io.flagship.core.ast.OperatorType;
import io.flagship.core.ast.RuleNode;
import io.flagship.core.strategy.OperatorRegistry;
import io.flagship.core.strategy.SemVer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Visitor implementation that structurally and semantically validates a rule AST.
 */
public final class RuleValidationVisitor implements RuleNodeVisitor<ValidationResult> {

    @Override
    public ValidationResult visit(CompositeRuleNode node) {
        List<String> errors = new ArrayList<>();
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            errors.add("CompositeRuleNode must have at least one child node");
        } else {
            for (RuleNode child : node.getChildren()) {
                if (child == null) {
                    errors.add("Child node in CompositeRuleNode cannot be null");
                } else {
                    ValidationResult childResult = child.accept(this);
                    if (!childResult.isValid()) {
                        errors.addAll(childResult.errors());
                    }
                }
            }
        }
        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }

    @Override
    public ValidationResult visit(LeafConditionNode node) {
        List<String> errors = new ArrayList<>();
        if (node.getAttribute() == null || node.getAttribute().isBlank()) {
            errors.add("Leaf condition attribute must not be blank");
        }
        if (node.getOperator() == null) {
            errors.add("Leaf condition operator must not be null");
            return ValidationResult.invalid(errors);
        }

        OperatorType op = node.getOperator();
        List<String> values = node.getValues();

        if (values == null || values.isEmpty()) {
            errors.add("Leaf condition must contain at least one target value for operator " + op);
            return ValidationResult.invalid(errors);
        }

        // Validate specific operator requirements
        if (op == OperatorType.REGEX) {
            for (String regex : values) {
                try {
                    Pattern.compile(regex);
                } catch (PatternSyntaxException e) {
                    errors.add("Invalid regex syntax '" + regex + "': " + e.getMessage());
                }
            }
        } else if (op == OperatorType.SEMVER_EQ || op == OperatorType.SEMVER_GT ||
                   op == OperatorType.SEMVER_GTE || op == OperatorType.SEMVER_LT ||
                   op == OperatorType.SEMVER_LTE) {
            for (String val : values) {
                if (SemVer.parseOrNull(val) == null) {
                    errors.add("Invalid SemVer string '" + val + "' for operator " + op);
                }
            }
        } else if (op == OperatorType.DATE_BEFORE || op == OperatorType.DATE_AFTER) {
            for (String val : values) {
                if (OperatorRegistry.parseInstant(val) == null) {
                    errors.add("Invalid ISO-8601 date string '" + val + "' for operator " + op);
                }
            }
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }
}
