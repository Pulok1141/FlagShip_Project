package io.flagship.core.visitor;

import java.util.Collections;
import java.util.List;

/**
 * Result of AST rule validation.
 */
public record ValidationResult(boolean isValid, List<String> errors) {

    public static ValidationResult valid() {
        return new ValidationResult(true, Collections.emptyList());
    }

    public static ValidationResult invalid(List<String> errors) {
        return new ValidationResult(false, List.copyOf(errors));
    }

    public static ValidationResult invalid(String error) {
        return new ValidationResult(false, List.of(error));
    }
}
