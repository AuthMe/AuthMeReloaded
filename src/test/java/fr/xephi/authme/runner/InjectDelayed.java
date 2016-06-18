package fr.xephi.authme.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks fields to instantiate with mocks after {@link BeforeInjecting} methods.
 *
 * @see DelayedInjectionRunner
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectDelayed {
}
