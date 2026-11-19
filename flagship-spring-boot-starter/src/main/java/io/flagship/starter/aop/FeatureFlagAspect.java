package io.flagship.starter.aop;

import io.flagship.core.model.EvaluationContext;
import io.flagship.core.model.EvaluationResult;
import io.flagship.starter.annotation.FeatureFlag;
import io.flagship.starter.client.FlagShipClient;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

@Aspect
public class FeatureFlagAspect {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagAspect.class);

    private final FlagShipClient client;
    private final EvaluationContextProvider contextProvider;

    public FeatureFlagAspect(FlagShipClient client, EvaluationContextProvider contextProvider) {
        this.client = client;
        this.contextProvider = contextProvider != null ? contextProvider : new DefaultEvaluationContextProvider();
    }

    @Around("@annotation(featureFlag)")
    public Object aroundFeatureFlag(ProceedingJoinPoint pjp, FeatureFlag featureFlag) throws Throwable {
        EvaluationContext ctx = contextProvider.resolveContext(pjp, featureFlag);
        String key = featureFlag.key();

        EvaluationResult result = client.evaluate(key, ctx, featureFlag.defaultValue());
        boolean isEnabled = isFlagEnabled(result.value());

        if (isEnabled) {
            log.debug("Feature flag [{}] ENABLED for user [{}], proceeding with invocation", key, ctx.userKey());
            return pjp.proceed();
        }

        log.debug("Feature flag [{}] DISABLED for user [{}], executing fallback", key, ctx.userKey());

        // Check fallbackMethod
        String fallbackMethodName = featureFlag.fallbackMethod();
        if (fallbackMethodName != null && !fallbackMethodName.isBlank()) {
            return invokeFallbackMethod(pjp, fallbackMethodName);
        }

        // Otherwise return coerced defaultValue
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Class<?> returnType = sig.getReturnType();
        return coerceDefaultValue(featureFlag.defaultValue(), returnType);
    }

    private boolean isFlagEnabled(Object val) {
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) {
            return "true".equalsIgnoreCase(s) || "enabled".equalsIgnoreCase(s) || "on".equalsIgnoreCase(s);
        }
        if (val instanceof Number n) return n.doubleValue() > 0;
        return false;
    }

    private Object invokeFallbackMethod(ProceedingJoinPoint pjp, String methodName) throws Throwable {
        Object target = pjp.getTarget();
        Class<?> targetClass = target.getClass();
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Class<?>[] paramTypes = sig.getParameterTypes();

        try {
            Method fallbackMethod = targetClass.getMethod(methodName, paramTypes);
            fallbackMethod.setAccessible(true);
            return fallbackMethod.invoke(target, pjp.getArgs());
        } catch (NoSuchMethodException e) {
            // Try no-arg fallback
            try {
                Method noArgMethod = targetClass.getMethod(methodName);
                noArgMethod.setAccessible(true);
                return noArgMethod.invoke(target);
            } catch (NoSuchMethodException ex) {
                log.warn("Fallback method [{}] not found on class [{}]: returning null", methodName, targetClass.getName());
                return null;
            }
        }
    }

    private Object coerceDefaultValue(String rawDefault, Class<?> returnType) {
        if (returnType == void.class || returnType == Void.class) {
            return null;
        }
        if (returnType == boolean.class || returnType == Boolean.class) {
            return Boolean.parseBoolean(rawDefault);
        }
        if (returnType == int.class || returnType == Integer.class) {
            try { return Integer.parseInt(rawDefault); } catch (NumberFormatException e) { return 0; }
        }
        if (returnType == long.class || returnType == Long.class) {
            try { return Long.parseLong(rawDefault); } catch (NumberFormatException e) { return 0L; }
        }
        if (returnType == double.class || returnType == Double.class) {
            try { return Double.parseDouble(rawDefault); } catch (NumberFormatException e) { return 0.0; }
        }
        if (returnType == String.class) {
            return rawDefault;
        }
        return null;
    }
}
