package fr.xephi.authme;

import java.lang.reflect.Field;

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
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(instance, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(
                String.format("Could not set value to field '%s' for instance '%s' of class '%s'",
                    fieldName, instance, clazz.getName()), e);
        }
    }
}
