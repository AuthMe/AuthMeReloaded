package fr.xephi.authme.initialization;

import javax.annotation.PostConstruct;
import javax.inject.Provider;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Helper class for functions relating to injecting.
 */
public class InjectionHelper {

    private InjectionHelper() {
    }

    /**
     * Returns the {@link Injection} for the given class, or null if none applicable.
     *
     * @param clazz the class to process
     * @param <T> the class' type
     * @return injection of the class or null if none detected
     */
    public static <T> Injection<T> getInjection(Class<T> clazz) {
        return firstNotNull(
            ConstructorInjection.provide(clazz),
            FieldInjection.provide(clazz),
            InstantiationFallback.provide(clazz));
    }

    /**
     * Validates and locates the given class' post construct method. Returns {@code null} if none present.
     *
     * @param clazz the class to search
     * @return post construct method, or null
     */
    public static Method getAndValidatePostConstructMethod(Class<?> clazz) {
        Method postConstructMethod = null;
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostConstruct.class)) {
                if (postConstructMethod != null) {
                    throw new IllegalStateException("Multiple methods with @PostConstruct on " + clazz);
                } else if (method.getParameterTypes().length > 0 || Modifier.isStatic(method.getModifiers())) {
                    throw new IllegalStateException("@PostConstruct method may not be static or have any parameters. "
                        + "Invalid method in " + clazz);
                } else if (method.getReturnType() != void.class) {
                    throw new IllegalStateException("@PostConstruct method must have return type void. "
                        + "Offending class: " + clazz);
                } else {
                    postConstructMethod = method;
                }
            }
        }
        return postConstructMethod;
    }

    @SafeVarargs
    private static <T> Injection<T> firstNotNull(Provider<? extends Injection<T>>... providers) {
        for (Provider<? extends Injection<T>> provider : providers) {
            Injection<T> object = provider.get();
            if (object != null) {
                return object;
            }
        }
        return null;
    }
}
