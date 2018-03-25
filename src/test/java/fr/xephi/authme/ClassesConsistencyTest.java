package fr.xephi.authme;

import ch.jalu.configme.properties.Property;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import fr.xephi.authme.data.captcha.CaptchaCodeStorage;
import fr.xephi.authme.datasource.AbstractSqlDataSource;
import fr.xephi.authme.datasource.Columns;
import fr.xephi.authme.datasource.columnshandler.DataSourceColumn;
import fr.xephi.authme.datasource.columnshandler.PlayerAuthColumn;
import fr.xephi.authme.datasource.mysqlextensions.MySqlExtension;
import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.process.register.executors.RegistrationMethod;
import fr.xephi.authme.security.crypts.Whirlpool;
import fr.xephi.authme.util.expiring.ExpiringMap;
import fr.xephi.authme.util.expiring.ExpiringSet;
import fr.xephi.authme.util.expiring.TimedCounter;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Contains consistency tests across all AuthMe classes.
 */
public class ClassesConsistencyTest {

    /** Contains all production classes. */
    private static final List<Class<?>> ALL_CLASSES =
        new ClassCollector(TestHelper.SOURCES_FOLDER, TestHelper.PROJECT_ROOT).collectClasses();

    /** Expiring structure types. */
    private static final Set<Class<?>> EXPIRING_STRUCTURES = ImmutableSet.of(
        ExpiringSet.class, ExpiringMap.class, TimedCounter.class, CaptchaCodeStorage.class);

    /** Immutable types, which are allowed to be used in non-private constants. */
    private static final Set<Class<?>> IMMUTABLE_TYPES = ImmutableSet.of(
        /* JDK */
        int.class, long.class, float.class, String.class, File.class, Enum.class, collectionsUnmodifiableList(),
        Charset.class,
        /* AuthMe */
        Property.class, RegistrationMethod.class, DataSourceColumn.class, PlayerAuthColumn.class,
        /* Guava */
        ImmutableMap.class, ImmutableList.class);

    /** Classes excluded from the field visibility test. */
    private static final Set<Class<?>> CLASSES_EXCLUDED_FROM_VISIBILITY_TEST = ImmutableSet.of(
        Whirlpool.class, // not our implementation, so we don't touch it
        MySqlExtension.class, // has immutable protected fields used by all children
        AbstractSqlDataSource.class, // protected members for inheritance
        Columns.class // uses non-static String constants, which is safe
    );

    /**
     * Checks that there aren't two classes with the same name; this is confusing and should be avoided.
     */
    @Test
    public void shouldNotHaveSameName() {
        // given
        Set<String> names = new HashSet<>();

        // when / then
        for (Class<?> clazz : ALL_CLASSES) {
            if (!names.add(clazz.getSimpleName())) {
                fail("Class with name '" + clazz.getSimpleName() + "' already encountered!");
            }
        }
    }

    /**
     * Checks that fields of classes are either private or static final fields of an immutable type.
     */
    @Test
    public void shouldHaveNonPrivateConstantsOnly() {
        // given / when
        Set<String> invalidFields = ALL_CLASSES.stream()
            .filter(clz -> !CLASSES_EXCLUDED_FROM_VISIBILITY_TEST.contains(clz))
            .map(Class::getDeclaredFields)
            .flatMap(Arrays::stream)
            .filter(f -> !f.getName().contains("$"))
            .filter(f -> hasIllegalFieldVisibility(f))
            .map(f -> formatField(f))
            .collect(Collectors.toSet());

        // then
        if (!invalidFields.isEmpty()) {
            fail("Found " + invalidFields.size() + " fields with non-private, mutable fields:\n- "
                + String.join("\n- ", invalidFields));
        }
    }

    private static boolean hasIllegalFieldVisibility(Field field) {
        final int modifiers = field.getModifiers();
        if (Modifier.isPrivate(modifiers)) {
            return false;
        } else if (!Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers)) {
            return true;
        }

        // Field is non-private, static and final
        Class<?> valueType;
        if (Collection.class.isAssignableFrom(field.getType()) || Map.class.isAssignableFrom(field.getType())) {
            // For collections/maps, need to check the actual type to ensure it's an unmodifiable implementation
            Object value = ReflectionTestUtils.getFieldValue(field, null);
            valueType = value.getClass();
        } else {
             valueType = field.getType();
        }

        // Field is static, final, and not private -> check that it is immutable type
        return IMMUTABLE_TYPES.stream()
            .noneMatch(immutableType -> immutableType.isAssignableFrom(valueType));
    }

    /**
     * Prints out the field with its modifiers.
     *
     * @param field the field to format
     * @return description of the field
     */
    private static String formatField(Field field) {
        String modifiersText = Modifier.toString(field.getModifiers());
        return String.format("[%s] %s %s %s", field.getDeclaringClass().getSimpleName(), modifiersText.trim(),
            field.getType().getSimpleName(), field.getName());
    }

    /**
     * Checks that classes with expiring collections (such as {@link ExpiringMap}) implement the {@link HasCleanup}
     * interface to regularly clean up expired data.
     */
    @Test
    public void shouldImplementHasCleanup() {
        // given / when / then
        for (Class<?> clazz : ALL_CLASSES) {
            if (hasExpiringCollectionAsField(clazz) && !EXPIRING_STRUCTURES.contains(clazz)) {
                assertThat("Class '" + clazz.getSimpleName() + "' has expiring collections, should implement HasCleanup",
                    HasCleanup.class.isAssignableFrom(clazz), equalTo(true));
            }
        }
    }

    private static boolean hasExpiringCollectionAsField(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (EXPIRING_STRUCTURES.stream().anyMatch(t -> t.isAssignableFrom(field.getType()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the concrete class of the unmodifiable list as returned by {@link Collections#unmodifiableList(List)}.
     */
    private static Class<?> collectionsUnmodifiableList() {
        return Collections.unmodifiableList(new ArrayList<>()).getClass();
    }
}
