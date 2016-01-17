package fr.xephi.authme.settings.custom;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.domain.SettingsClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Test for {@link SettingsClass} implementations.
 */
public class SettingsClassConsistencyTest {

    private static final String SETTINGS_FOLDER = "src/main/java/fr/xephi/authme/settings/custom";
    private static List<Class<? extends SettingsClass>> classes;

    @BeforeClass
    public static void scanForSettingsClasses() {
        File settingsFolder = new File(SETTINGS_FOLDER);
        File[] filesInFolder = settingsFolder.listFiles();
        if (filesInFolder == null || filesInFolder.length == 0) {
            throw new IllegalStateException("Could not read folder '" + SETTINGS_FOLDER + "'. Is it correct?");
        }

        classes = new ArrayList<>();
        for (File file : filesInFolder) {
            Class<? extends SettingsClass> clazz = getSettingsClassFromFile(file);
            if (clazz != null) {
                classes.add(clazz);
            }
        }
        System.out.println("Found " + classes.size() + " SettingsClass implementations");
    }

    /**
     * Make sure that all {@link Property} instances we define are in public, static, final fields.
     */
    @Test
    public void shouldHavePublicStaticFinalFields() {
        for (Class<?> clazz : classes) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (Property.class.isAssignableFrom(field.getType())) {
                    String fieldName = "Field " + clazz.getSimpleName() + "#" + field.getName();
                    assertThat(fieldName + " should be public, static, and final",
                        isValidConstantField(field), equalTo(true));
                }
            }
        }
    }

    /**
     * Make sure that no properties use the same path.
     */
    @Test
    public void shouldHaveUniquePaths() {
        Set<String> paths = new HashSet<>();
        for (Class<?> clazz : classes) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (Property.class.isAssignableFrom(field.getType())) {
                    Property<?> property =
                        (Property<?>) ReflectionTestUtils.getFieldValue(clazz, null, field.getName());
                    if (paths.contains(property.getPath())) {
                        fail("Path '" + property.getPath() + "' should be used by only one constant");
                    }
                    paths.add(property.getPath());
                }
            }
        }
    }

    @Test
    public void shouldHaveHiddenDefaultConstructorOnly() {
        for (Class<?> clazz : classes) {
            Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            assertThat(clazz + " should only have one constructor",
                constructors, arrayWithSize(1));
            assertThat("Constructor of " + clazz + " is private",
                Modifier.isPrivate(constructors[0].getModifiers()), equalTo(true));
        }
    }

    private static boolean isValidConstantField(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers);
    }

    private static Class<? extends SettingsClass> getSettingsClassFromFile(File file) {
        String fileName = file.getPath();
        String className = fileName
            .substring("src/main/java/".length(), fileName.length() - ".java".length())
            .replace(File.separator, ".");
        try {
            Class<?> clazz = SettingsClassConsistencyTest.class.getClassLoader().loadClass(className);
            if (SettingsClass.class.isAssignableFrom(clazz)) {
                return (Class<? extends SettingsClass>) clazz;
            }
            return null;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not load class '" + className + "'", e);
        }
    }

}
