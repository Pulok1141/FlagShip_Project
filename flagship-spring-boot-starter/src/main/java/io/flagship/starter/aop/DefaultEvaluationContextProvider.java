package io.flagship.starter.aop;

import io.flagship.core.model.EvaluationContext;
import io.flagship.starter.annotation.FeatureFlag;
import io.flagship.starter.annotation.UserKey;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class DefaultEvaluationContextProvider implements EvaluationContextProvider {

    @Override
    public EvaluationContext resolveContext(ProceedingJoinPoint pjp, FeatureFlag featureFlag) {
        Object[] args = pjp.getArgs();
        if (args == null || args.length == 0) {
            return EvaluationContext.anonymous();
        }

        // 1. Check if an EvaluationContext was passed directly as an argument
        for (Object arg : args) {
            if (arg instanceof EvaluationContext ctx) {
                return ctx;
            }
        }

        // 2. Check if any parameter has @UserKey annotation
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();

        String userKey = null;
        Map<String, Object> attributes = new HashMap<>();

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null) continue;

            for (Annotation ann : paramAnnotations[i]) {
                if (ann instanceof UserKey) {
                    userKey = arg.toString();
                    break;
                }
            }

            // If parameter name is "userKey" or "userId"
            String paramName = sig.getParameterNames() != null && sig.getParameterNames().length > i
                    ? sig.getParameterNames()[i] : null;

            if (userKey == null && paramName != null) {
                if ("userKey".equalsIgnoreCase(paramName) || "userId".equalsIgnoreCase(paramName)) {
                    userKey = arg.toString();
                } else {
                    attributes.put(paramName, arg);
                }
            } else if (paramName != null) {
                attributes.put(paramName, arg);
            }
        }

        return EvaluationContext.of(userKey != null ? userKey : "anonymous", attributes);
    }
}
