package io.flagship.starter.annotation;

import java.lang.annotation.*;

/**
 * Marks a method parameter as the user identifier for FlagShip evaluation context.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UserKey {
}
