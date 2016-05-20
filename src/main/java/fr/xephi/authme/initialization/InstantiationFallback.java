package fr.xephi.authme.initialization;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Fallback instantiation method for classes with an accessible no-args constructor
 * and no elements whatsoever annotated with {@link Inject} or {@link PostConstruct}.
 */
public class InstantiationFallback<T> implements Injection<T> {

    private final Constructor<T> constructor;

    private InstantiationFallback(Constructor<T> constructor) {
        this.constructor = constructor;
    }

    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[0];
    }

    @Override
    public Class<?>[] getDependencyAnnotations() {
        return new Class<?>[0];
    }

    @Override
    public T instantiateWith(Object... values) {
        if (values == null || values.length > 0) {
            throw new UnsupportedOperationException("Instantiation fallback cannot have parameters");
        }
        try {
            return constructor.newInstance();
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    /**
     * Returns an instantiation fallback if the class is applicable.
     *
     * @param clazz the class
     * @param <T> the class' type
     * @return instantiation fallback provider for the given class, or null if not applicable
     */
    public static <T> Provider<InstantiationFallback<T>> provide(final Class<T> clazz) {
        return new Provider<InstantiationFallback<T>>() {
            @Override
            public InstantiationFallback<T> get() {
                Constructor<T> noArgsConstructor = getNoArgsConstructor(clazz);
                // Return fallback only if we have no args constructor and no @Inject annotation anywhere
                if (noArgsConstructor != null
                    && !isInjectionAnnotationPresent(clazz.getDeclaredConstructors())
                    && !isInjectionAnnotationPresent(clazz.getDeclaredFields())
                    && !isInjectionAnnotationPresent(clazz.getDeclaredMethods())) {
                    return new InstantiationFallback<>(noArgsConstructor);
                }
                return null;
            }
        };
    }

    private static <T> Constructor<T> getNoArgsConstructor(Class<T> clazz) {
        try {
            // Note ljacqu 20160504: getConstructor(), unlike getDeclaredConstructor(), only considers public members
            return clazz.getConstructor();
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static <A extends AccessibleObject> boolean isInjectionAnnotationPresent(A[] accessibles) {
        for (A accessible : accessibles) {
            if (accessible.isAnnotationPresent(Inject.class) || accessible.isAnnotationPresent(PostConstruct.class)) {
                return true;
            }
        }
        return false;
    }
}
