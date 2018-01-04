package fr.xephi.authme.initialization.circulardependency;

import ch.jalu.injector.Injector;
import ch.jalu.injector.utils.ReflectionUtils;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Fix for circular dependencies: after initialization this class can be called
 * to inject dependencies via {@link InjectAfterInitialization} methods.
 */
public class CircularDependencyInitializer {

    @Inject
    private Injector injector;

    CircularDependencyInitializer() {
    }

    /**
     * Processes all known {@link HasCircularDependency} classes, invoking all methods
     * annotated with {@link InjectAfterInitialization}.
     */
    public void initializeCircularDependencies() {
        for (HasCircularDependency hasCircularDependency : injector.retrieveAllOfType(HasCircularDependency.class)) {
            processClass(hasCircularDependency);
        }
    }

    private void processClass(HasCircularDependency object) {
        for (Method method : object.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(InjectAfterInitialization.class)) {
                Object resolvedObject = resolveParameterForMethodOrThrow(method);
                ReflectionUtils.invokeMethod(method, object, resolvedObject);
            }
        }
    }

    /**
     * Validates that the given method is a valid {@link InjectAfterInitialization} method
     * and resolves the parameter it should be passed (assumes a singleton of the given type is registered
     * in the injector). Throws an exception if the parameter type is not a registered singleton.
     *
     * @param method the method to process
     * @return object to pass to the initializer method
     */
    private Object resolveParameterForMethodOrThrow(Method method) {
        if (method.getParameterCount() != 1) {
            throw new IllegalStateException("Method " + method.getDeclaringClass() + "#" + method.getName()
                + " should have one parameter only");
        } else if (!Modifier.isPublic(method.getModifiers())) {
            throw new IllegalStateException("Method " + method.getDeclaringClass() + "#" + method.getName()
                + " should be public");
        } else if (method.getReturnType() != void.class) {
            throw new IllegalStateException("Method " + method.getDeclaringClass() + "#" + method.getName()
                + " must return void");
        }

        final Class<?> requiredType = method.getParameterTypes()[0];
        final Object resolvedObject = injector.getIfAvailable(requiredType);
        if (resolvedObject == null) {
            throw new IllegalStateException("Failed to get parameter of type '" + requiredType
                + "' for @InjectAfterInitialization method " + method.getDeclaringClass() + "#" + method.getName());
        }
        return resolvedObject;
    }
}
