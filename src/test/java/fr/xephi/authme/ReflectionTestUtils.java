package fr.xephi.authme;

import ch.jalu.injector.handlers.postconstruct.PostConstructMethodInvoker;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static java.lang.String.format;

/**
 * Offers reflection functionality to set up tests. Use only when absolutely necessary.
 */
public final class ReflectionTestUtils {

    private ReflectionTestUtils() {
        // Util class
    }

    /**
     * Sets the field of a given object to a new value with reflection.
     *
     * @param clazz the class declaring the field
     * @param instance the instance to modify (pass null for static fields)
     * @param fieldName the field name
     * @param value the value to set the field to
     */
    public static <T> void setField(Class<? super T> clazz, T instance, String fieldName, Object value) {
        try {
            Field field = getField(clazz, fieldName);
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException(
                format("Could not set value to field '%s' for instance '%s' of class '%s'",
                    fieldName, instance, clazz.getName()), e);
        }
    }

    /**
     * Sets the field on the given instance to the new value.
     *
     * @param instance the instance to modify
     * @param fieldName the field name
     * @param value the value to set the field to
     */
    @SuppressWarnings("unchecked")
    public static void setField(Object instance, String fieldName, Object value) {
        setField((Class) instance.getClass(), instance, fieldName, value);
    }

    private static <T> Field getField(Class<T> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new UnsupportedOperationException(format("Could not get field '%s' from class '%s'",
                fieldName, clazz.getName()), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T, V> V getFieldValue(Class<T> clazz, T instance, String fieldName) {
        Field field = getField(clazz, fieldName);
        return getFieldValue(field, instance);
    }

    @SuppressWarnings("unchecked")
    public static <V> V getFieldValue(Field field, Object instance) {
        field.setAccessible(true);
        try {
            return (V) field.get(instance);
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException("Could not get value of field '" + field.getName() + "'", e);
        }
    }

    /**
     * Returns the method on the given class with the supplied parameter types.
     *
     * @param clazz the class to retrieve a method from
     * @param methodName the name of the method
     * @param parameterTypes the parameter types the method to retrieve has
     * @return the method of the class, set to be accessible
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException("Could not retrieve method '" + methodName + "' from class '"
                + clazz.getName() + "'");
        }
    }

    /**
     * Invokes the given method on the provided instance with the given parameters.
     *
     * @param method the method to invoke
     * @param instance the instance to invoke the method on (null for static methods)
     * @param parameters the parameters to pass to the method
     * @param <V> return value of the method
     * @return method return value
     */
    @SuppressWarnings("unchecked")
    public static <V> V invokeMethod(Method method, Object instance, Object... parameters) {
        method.setAccessible(true);
        try {
            return (V) method.invoke(instance, parameters);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new UnsupportedOperationException("Could not invoke method '" + method + "'", e);
        }
    }

    /**
     * Runs all methods annotated with {@link javax.annotation.PostConstruct} on the given instance
     * (including such methods on superclasses).
     *
     * @param instance the instance to process
     */
    public static void invokePostConstructMethods(Object instance) {
        // Use the implementation of the injector to invoke all @PostConstruct methods the same way
        new PostConstructMethodInvoker().postProcess(instance, null, null);
    }

    /**
     * Creates a new instance of the given class, using a no-args constructor (which may be hidden).
     *
     * @param clazz the class to instantiate
     * @param <T> the class' type
     * @return the created instance
     */
    public static <T> T newInstance(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new UnsupportedOperationException("Could not invoke no-args constructor of class " + clazz, e);
        }
    }
}
