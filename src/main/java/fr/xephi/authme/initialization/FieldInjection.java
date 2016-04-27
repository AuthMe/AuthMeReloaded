package fr.xephi.authme.initialization;

import com.google.common.base.Preconditions;

import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Functionality for field injection.
 */
class FieldInjection<T> implements Injection<T> {

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
                fields[i].set(instance, values[i]);
            } catch (IllegalAccessException e) {
                throw new UnsupportedOperationException(e);
            }
        }
        return instance;
    }

    private static Class<?> getFirstNonInjectAnnotation(Field field) {
        for (Annotation annotation : field.getAnnotations()) {
            if (annotation.annotationType() != Inject.class) {
                return annotation.annotationType();
            }
        }
        return null;
    }

    public static <T> Provider<FieldInjection<T>> provide(final Class<T> clazz) {
        return new Provider<FieldInjection<T>>() {
            @Override
            public FieldInjection<T> get() {
                Constructor<T> constructor = getDefaultConstructor(clazz);
                if (constructor == null) {
                    return null;
                }
                List<Field> fields = getInjectionFields(clazz);
                return fields == null ? null : new FieldInjection<>(constructor, fields);
            }
        };
    }

    private static List<Field> getInjectionFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                field.setAccessible(true);
                fields.add(field);
            }
        }
        return fields;
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
