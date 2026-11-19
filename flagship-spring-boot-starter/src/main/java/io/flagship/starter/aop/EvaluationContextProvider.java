package io.flagship.starter.aop;

import io.flagship.core.model.EvaluationContext;
import io.flagship.starter.annotation.FeatureFlag;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Pluggable SPI for resolving evaluation context (userKey, attributes) during AOP intercept.
 */
public interface EvaluationContextProvider {

    EvaluationContext resolveContext(ProceedingJoinPoint pjp, FeatureFlag featureFlag);
}
