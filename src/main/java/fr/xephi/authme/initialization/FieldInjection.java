package fr.xephi.authme.initialization;

import com.google.common.base.Preconditions;

import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Functionality for field injection.
 */
public class FieldInjection<T> implements Injection<T> {

    private final Field[] fields;
    private final Constructor<T> defaultConstructor;

    private FieldInjection(Constructor<T> defaultConstructor, Collection<Field> fields) {
        this.fields = fields.toArray(new Field[fields.size()]);
        this.defaultConstructor = defaultConstructor;
    }

    @Override
    public Class<?>[] getDependencies() {
        Class<?>[] types = new Class<?>[fields.length];
        for (int i = 0; i < fields.length; ++i) {
            types[i] = fields[i].getType();
        }
        return types;
    }

    @Override
    public Class<?>[] getDependencyAnnotations() {
        Class<?>[] annotations = new Class<?>[fields.length];
        for (int i = 0; i < fields.length; ++i) {
            annotations[i] = getFirstNonInjectAnnotation(fields[i]);
        }
        return annotations;
    }

    @Override
    public T instantiateWith(Object... values) {
        Preconditions.checkArgument(values.length == fields.length,
            "The number of values must be equal to the number of fields");

        T instance;
        try {
            instance = defaultConstructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new UnsupportedOperationException(e);
        }

        for (int i = 0; i < fields.length; ++i) {
            try {
                Preconditions.checkNotNull(values[i]);
                fields[i].set(instance, values[i]);
            } catch (IllegalAccessException e) {
                throw new UnsupportedOperationException(e);
            }
        }
        return instance;
    }

    /**
     * Returns a provider for a {@code FieldInjection<T>} instance, i.e. a provides an object
     * with which field injection can be performed on the given class if applicable. The provided
     * value is {@code null} if field injection cannot be applied to the class.
     *
     * @param clazz the class to provide field injection for
     * @param <T> the class' type
     * @return field injection provider for the given class, or null if not applicable
     */
    public static <T> Provider<FieldInjection<T>> provide(final Class<T> clazz) {
        return new Provider<FieldInjection<T>>() {
            @Override
            public FieldInjection<T> get() {
                Constructor<T> constructor = getDefaultConstructor(clazz);
                if (constructor == null) {
                    return null;
                }
                List<Field> fields = getInjectionFields(clazz);
                return fields.isEmpty() ? null : new FieldInjection<>(constructor, fields);
            }
        };
    }

    private static List<Field> getInjectionFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                if (Modifier.isStatic(field.getModifiers())) {
                    throw new IllegalStateException(String.format("Field '%s' in class '%s' is static but "
                        + "annotated with @Inject", field.getName(), clazz.getSimpleName()));
                }
                field.setAccessible(true);
                fields.add(field);
            }
        }
        return fields;
    }

    private static Class<?> getFirstNonInjectAnnotation(Field field) {
        for (Annotation annotation : field.getAnnotations()) {
            if (annotation.annotationType() != Inject.class) {
                return annotation.annotationType();
            }
        }
        return null;
    }

    private static <T> Constructor<T> getDefaultConstructor(Class<T> clazz) {
        try {
            Constructor<?> defaultConstructor = clazz.getDeclaredConstructor();
            defaultConstructor.setAccessible(true);
            return (Constructor<T>) defaultConstructor;
        } catch (NoSuchMethodException ignore) {
            // no default constructor available
        }
        return null;
    }
}
