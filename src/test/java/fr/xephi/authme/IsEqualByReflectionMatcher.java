package fr.xephi.authme;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.fail;

/**
 * Matcher which checks with reflection that all fields have the same value.
 * This matcher considers all non-static fields until the Object parent.
 */
public final class IsEqualByReflectionMatcher<T> extends TypeSafeMatcher<T> {

    private final T expected;

    private IsEqualByReflectionMatcher(T expected) {
        this.expected = expected;
    }

    /**
     * Creates a matcher that checks if all fields are the same as on the {@code expected} object.
     *
     * @param expected the object to match
     * @param <T> the object's type
     * @return the matcher for the expected object
     */
    public static <T> Matcher<T> hasEqualValuesOnAllFields(T expected) {
        return new IsEqualByReflectionMatcher<>(expected);
    }

    @Override
    protected boolean matchesSafely(T item) {
        return assertAreFieldsEqual(item);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("parameters " + expected);
    }

    private boolean assertAreFieldsEqual(T item) {
        if (expected.getClass() != item.getClass()) {
            fail("Classes don't match, got " + expected.getClass().getSimpleName()
                + " and " + item.getClass().getSimpleName());
            return false;
        }

        List<Field> fieldsToCheck = getAllFields(expected);
        for (Field field : fieldsToCheck) {
            Object lhsValue = ReflectionTestUtils.getFieldValue(field, expected);
            Object rhsValue = ReflectionTestUtils.getFieldValue(field, item);
            if (!Objects.equals(lhsValue, rhsValue)) {
                fail("Field '" + field.getName() + "' does not have same value: '"
                    + lhsValue + "' vs. '" + rhsValue + "'");
                return false;
            }
        }
        return true;
    }

    private static List<Field> getAllFields(Object object) {
        List<Field> fields = new ArrayList<>();
        Class<?> currentClass = object.getClass();
        while (currentClass != null) {
            for (Field f : currentClass.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()) && !f.isSynthetic()) {
                    fields.add(f);
                }
            }
            if (currentClass == Object.class) {
                break;
            }
            currentClass = currentClass.getSuperclass();
        }
        return fields;
    }
}
