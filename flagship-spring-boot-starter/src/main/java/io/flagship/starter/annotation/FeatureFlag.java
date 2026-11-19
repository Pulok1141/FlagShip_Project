package io.flagship.starter.annotation;

import java.lang.annotation.*;

/**
 * Declarative Feature Flag Guard for Spring Services and Controllers (FR4).
 * If the feature flag evaluates to true, the annotated method executes normally.
 * If false, returns defaultValue or executes fallbackMethod.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FeatureFlag {

    /**
     * Unique key of the feature flag to evaluate.
     */
    String key();

    /**
     * Fallback value returned if the flag is disabled and no fallbackMethod is defined.
     */
    String defaultValue() default "false";

    /**
     * Optional method name on the same bean to invoke when the flag is disabled.
     */
    String fallbackMethod() default "";
}
