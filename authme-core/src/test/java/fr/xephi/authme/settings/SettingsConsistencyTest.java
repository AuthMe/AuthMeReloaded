package fr.xephi.authme.settings;

import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.properties.EnumProperty;
import ch.jalu.configme.properties.Property;
import com.google.common.collect.ImmutableSet;
import fr.xephi.authme.settings.properties.AuthMeSettingsRetriever;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.xephi.authme.ReflectionTestUtils.getFieldValue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests the consistency of the settings configuration.
 */
class SettingsConsistencyTest {

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

    @BeforeAll
    static void buildConfigurationData() {
        configurationData = AuthMeSettingsRetriever.buildConfigurationData();
    }

    @Test
    void shouldHaveCommentOnEachProperty() {
        // given
        List<Property<?>> properties = configurationData.getProperties();

        // when / then
        for (Property<?> property : properties) {
            if (configurationData.getCommentsForSection(property.getPath()).isEmpty()) {
                fail("No comment defined for " + property);
            }
        }
    }

    @Test
    void shouldNotHaveVeryLongCommentLines() {
        // given
        Map<String, List<String>> commentEntries = configurationData.getAllComments();
        List<String> badPaths = new ArrayList<>(0);

        // when
        for (Map.Entry<String, List<String>> commentEntry : commentEntries.entrySet()) {
            for (String comment : commentEntry.getValue()) {
                if (comment.length() > MAX_COMMENT_LENGTH) {
                    badPaths.add(commentEntry.getKey());
                    break;
                }
            }
        }

        // then
        if (!badPaths.isEmpty()) {
            fail("Comment lines should not be longer than " + MAX_COMMENT_LENGTH + " chars, "
                + "but found too long comments for paths:\n- "
                + String.join("\n- ", badPaths));
        }
    }


    /**
     * Checks that enum properties have all possible enum values listed in their comment
     * so the user knows which values are available.
     */
    @Test
    void shouldMentionAllEnumValues() {
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
            Class clazz = property.getDefaultValue().getClass();
            // Check if is anonymous class in case enum implements methods, e.g. AllowFlightRestoreType
            return clazz.isAnonymousClass() ? clazz.getEnclosingClass() : clazz;
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
