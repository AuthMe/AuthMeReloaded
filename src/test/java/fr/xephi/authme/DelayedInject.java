package fr.xephi.authme;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks fields to be instantiated right before a method is invoked on them for the first time.
 *
 * @see DelayedInjectionRunner
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DelayedInject {
}
