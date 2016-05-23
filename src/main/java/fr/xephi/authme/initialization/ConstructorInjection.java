package fr.xephi.authme.initialization;

import com.google.common.base.Preconditions;

import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Functionality for constructor injection.
 */
public class ConstructorInjection<T> implements Injection<T> {

    private final Constructor<T> constructor;

    private ConstructorInjection(Constructor<T> constructor) {
        this.constructor = constructor;
    }

    @Override
    public Class<?>[] getDependencies() {
        return constructor.getParameterTypes();
    }

    @Override
    public Class<?>[] getDependencyAnnotations() {
        Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();
        Class<?>[] annotations = new Class<?>[parameterAnnotations.length];
        for (int i = 0; i < parameterAnnotations.length; ++i) {
            annotations[i] = parameterAnnotations[i].length > 0
                ? parameterAnnotations[i][0].annotationType()
                : null;
        }
        return annotations;
    }

    @Override
    public T instantiateWith(Object... values) {
        validateNoNullValues(values);
        try {
            return constructor.newInstance(values);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static <T> Provider<ConstructorInjection<T>> provide(final Class<T> clazz) {
        return new Provider<ConstructorInjection<T>>() {
            @Override
            public ConstructorInjection<T> get() {
                Constructor<T> constructor = getInjectionConstructor(clazz);
                return constructor == null ? null : new ConstructorInjection<>(constructor);
            }
        };
    }


    /**
     * Gets the first found constructor annotated with {@link Inject} of the given class
     * and marks it as accessible.
     *
     * @param clazz the class to process
     * @param <T> the class' type
     * @return injection constructor for the class, null if not applicable
     */
    @SuppressWarnings("unchecked")
    private static <T> Constructor<T> getInjectionConstructor(Class<T> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(Inject.class)) {
                constructor.setAccessible(true);
                return (Constructor<T>) constructor;
            }
        }
        return null;
    }

    private static void validateNoNullValues(Object[] array) {
        for (Object entry : array) {
            Preconditions.checkNotNull(entry);
        }
    }

}
