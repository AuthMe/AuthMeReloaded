package fr.xephi.authme.settings;

import ch.jalu.configme.SectionComments;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.properties.EnumProperty;
import ch.jalu.configme.properties.Property;
import com.google.common.collect.ImmutableSet;
import fr.xephi.authme.ClassCollector;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.settings.properties.AuthMeSettingsRetriever;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.xephi.authme.ReflectionTestUtils.getFieldValue;
import static org.junit.Assert.fail;

/**
 * Tests the consistency of the settings configuration.
 */
public class SettingsConsistencyTest {

    /**
     * Maximum characters one comment line may have (prevents horizontal scrolling).
     */
    private static final int MAX_COMMENT_LENGTH = 90;

    /**
     * Properties to exclude from the enum check.
     */
    private static final Set<Property<?>> EXCLUDED_ENUM_PROPERTIES =
        ImmutableSet.of(SecuritySettings.PASSWORD_HASH, SecuritySettings.LEGACY_HASHES);

    private static ConfigurationData configurationData;

    @BeforeClass
    public static void buildConfigurationData() {
        configurationData = AuthMeSettingsRetriever.buildConfigurationData();
    }

    @Test
    public void shouldHaveCommentOnEachProperty() {
        // given
        List<Property<?>> properties = configurationData.getProperties();

        // when / then
        for (Property<?> property : properties) {
            if (configurationData.getCommentsForSection(property.getPath()).length == 0) {
                fail("No comment defined for " + property);
            }
        }
    }

    @Test
    public void shouldNotHaveVeryLongCommentLines() {
        // given
        List<Property<?>> properties = configurationData.getProperties();
        List<Property<?>> badProperties = new ArrayList<>();

        // when
        for (Property<?> property : properties) {
            for (String comment : configurationData.getCommentsForSection(property.getPath())) {
                if (comment.length() > MAX_COMMENT_LENGTH) {
                    badProperties.add(property);
                    break;
                }
            }
        }

        // then
        if (!badProperties.isEmpty()) {
            fail("Comment lines should not be longer than " + MAX_COMMENT_LENGTH + " chars, "
                + "but found too long comments for:\n- "
                + badProperties.stream().map(Property::getPath).collect(Collectors.joining("\n- ")));
        }
    }

    @Test
    public void shouldNotHaveVeryLongSectionCommentLines() {
        // given
        List<Method> sectionCommentMethods = getSectionCommentMethods();
        Set<Method> badMethods = new HashSet<>();

        // when
        for (Method method : sectionCommentMethods) {
            boolean hasTooLongLine = getSectionComments(method).stream()
                .anyMatch(line -> line.length() > MAX_COMMENT_LENGTH);
            if (hasTooLongLine) {
                badMethods.add(method);
            }
        }

        // then
        if (!badMethods.isEmpty()) {
            String methodList = badMethods.stream()
                .map(m -> m.getName() + " in " + m.getDeclaringClass().getSimpleName())
                .collect(Collectors.joining("\n- "));
            fail("Found SectionComments methods with too long comments:\n- " + methodList);
        }
    }

    /**
     * Gets all {@link SectionComments} methods from {@link SettingsHolder} implementations.
     */
    @SuppressWarnings("unchecked")
    private List<Method> getSectionCommentMethods() {
        // Find all SettingsHolder classes
        List<Class<? extends SettingsHolder>> settingsClasses =
            new ClassCollector(TestHelper.SOURCES_FOLDER, TestHelper.PROJECT_ROOT + "settings/properties/")
                .collectClasses(SettingsHolder.class);
        checkArgument(!settingsClasses.isEmpty(), "Could not find any SettingsHolder classes");

        // Find all @SectionComments methods in these classes
        return settingsClasses.stream()
            .map(Class::getDeclaredMethods)
            .flatMap(Arrays::stream)
            .filter(method -> method.isAnnotationPresent(SectionComments.class))
            .collect(Collectors.toList());
    }

    /**
     * Returns all comments returned from the given SectionComments method, flattened into one list.
     *
     * @param sectionCommentsMethod the method whose comments should be retrieved
     * @return flattened list of all comments provided by the method
     */
    private static List<String> getSectionComments(Method sectionCommentsMethod) {
        // @SectionComments methods are static
        Map<String, String[]> comments = ReflectionTestUtils.invokeMethod(sectionCommentsMethod, null);
        return comments.values().stream()
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
    }

    /**
     * Checks that enum properties have all possible enum values listed in their comment
     * so the user knows which values are available.
     */
    @Test
    public void shouldMentionAllEnumValues() {
        // given
        Map<Property<?>, Enum<?>> invalidEnumProperties = new HashMap<>();

        for (Property<?> property : configurationData.getProperties()) {
            // when
            Class<? extends Enum<?>> enumClass = getEnumClass(property);
            if (enumClass != null && !EXCLUDED_ENUM_PROPERTIES.contains(property)) {
                String comments = String.join("\n", configurationData.getCommentsForSection(property.getPath()));
                Arrays.stream(enumClass.getEnumConstants())
                    .filter(e -> !comments.contains(e.name()) && !isDeprecated(e))
                    .findFirst()
                    .ifPresent(e -> invalidEnumProperties.put(property, e));
            }
        }

        // then
        if (!invalidEnumProperties.isEmpty()) {
            String invalidEnums = invalidEnumProperties.entrySet().stream()
                .map(e -> e.getKey() + " does not mention " + e.getValue() + " and possibly others")
                .collect(Collectors.joining("\n- "));

            fail("Found enum properties that do not list all entries in the comments:\n- " + invalidEnums);
        }
    }

    /**
     * Returns the enum class the property holds values for, if applicable.
     *
     * @param property the property to get the enum class from
     * @return the enum class, or null if not available
     */
    private static Class<? extends Enum<?>> getEnumClass(Property<?> property) {
        if (property instanceof EnumProperty<?>) {
            return getFieldValue(EnumProperty.class, (EnumProperty<?>) property, "clazz");
        } else if (property instanceof EnumSetProperty<?>) {
            return getFieldValue(EnumSetProperty.class, (EnumSetProperty<?>) property, "enumClass");
        }
        return null;
    }

    private static boolean isDeprecated(Enum<?> enumValue) {
        try {
            return enumValue.getDeclaringClass().getField(enumValue.name()).isAnnotationPresent(Deprecated.class);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Could not fetch field for enum '" + enumValue
                + "' in " + enumValue.getDeclaringClass());
        }
    }
}
