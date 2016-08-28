package fr.xephi.authme;

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
     * Set the field of a given object to a new value with reflection.
     *
     * @param clazz The class of the object
     * @param instance The instance to modify (pass null for static fields)
     * @param fieldName The field name
     * @param value The value to set the field to
     */
    public static <T> void setField(Class<T> clazz, T instance, String fieldName, Object value) {
        try {
            Field field = getField(clazz, instance, fieldName);
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException(
                format("Could not set value to field '%s' for instance '%s' of class '%s'",
                    fieldName, instance, clazz.getName()), e);
        }
    }

    private static <T> Field getField(Class<T> clazz, T instance, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new UnsupportedOperationException(format("Could not get field '%s' for instance '%s' of class '%s'",
                fieldName, instance, clazz.getName()), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T, V> V getFieldValue(Class<T> clazz, T instance, String fieldName) {
        Field field = getField(clazz, instance, fieldName);
        try {
            return (V) field.get(instance);
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException("Could not get value of field '" + fieldName + "'", e);
        }
    }

    /**
     * Return the method on the given class with the supplied parameter types.
     *
     * @param clazz The class to retrieve a method from
     * @param methodName The name of the method
     * @param parameterTypes The parameter types the method to retrieve has
     *
     * @return The method of the class, set to be accessible
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

    public static Object invokeMethod(Method method, Object instance, Object... parameters) {
        method.setAccessible(true);
        try {
            return method.invoke(instance, parameters);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new UnsupportedOperationException("Could not invoke method '" + method + "'", e);
        }
    }
}
