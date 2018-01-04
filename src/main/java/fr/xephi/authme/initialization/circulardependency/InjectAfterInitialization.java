package fr.xephi.authme.initialization.circulardependency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method that should be invoked once injection has taken place.
 * This is a fix for circular dependencies.
 * <p>
 * Methods with this annotation must have exactly one parameter whose type is a singleton
 * registered in the injector. Classes with such methods must implement the {@link HasCircularDependency}
 * marker interface.
 *
 * @see CircularDependencyInitializer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface InjectAfterInitialization {
}
