package fr.xephi.authme.settings.properties;

import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.properties.Property;
import fr.xephi.authme.ClassCollector;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Test for {@link SettingsHolder} implementations.
 */
public class SettingsClassConsistencyTest {

    private static final String SETTINGS_FOLDER = TestHelper.PROJECT_ROOT + "settings/properties";
    private static List<Class<? extends SettingsHolder>> classes;

    @BeforeClass
    public static void scanForSettingsClasses() {
        ClassCollector collector = new ClassCollector(TestHelper.SOURCES_FOLDER, SETTINGS_FOLDER);
        classes = collector.collectClasses(SettingsHolder.class);

        if (classes.isEmpty()) {
            throw new IllegalStateException("Did not find any SettingsHolder classes. Is the folder correct?");
        }
        System.out.println("Found " + classes.size() + " SettingsHolder implementations");
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
                        ReflectionTestUtils.getFieldValue(clazz, null, field.getName());
                    if (!paths.add(property.getPath())) {
                        fail("Path '" + property.getPath() + "' should be used by only one constant");
                    }
                }
            }
        }
    }

    /**
     * Checks that {@link AuthMeSettingsRetriever} returns a ConfigurationData with all
     * available SettingsHolder classes.
     */
    @Test
    public void shouldHaveAllClassesInConfigurationData() {
        // given
        long totalProperties = classes.stream()
            .map(Class::getDeclaredFields)
            .flatMap(Arrays::stream)
            .filter(field -> Property.class.isAssignableFrom(field.getType()))
            .count();

        // when
        ConfigurationData configData = AuthMeSettingsRetriever.buildConfigurationData();

        // then
        assertThat("ConfigurationData should have " + totalProperties + " properties (as found manually)",
            configData.getProperties(), hasSize((int) totalProperties));
    }

    private static boolean isValidConstantField(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers);
    }
}
