package fr.xephi.authme.settings;

import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.properties.EnumProperty;
import ch.jalu.configme.properties.Property;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.settings.properties.AuthMeSettingsRetriever;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * Exclusions for the enum in comments check. Use {@link Exclude#ALL}
     * to skip an entire property from being checked.
     */
    private static final Multimap<Property<?>, Enum<?>> EXCLUDED_ENUMS =
        ImmutableSetMultimap.<Property<?>, Enum<?>>builder()
            .put(DatabaseSettings.BACKEND, DataSourceType.FILE)
            .put(SecuritySettings.PASSWORD_HASH, Exclude.ALL)
            .put(SecuritySettings.LEGACY_HASHES, Exclude.ALL)
            .build();

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
            if (enumClass != null) {
                String comments = String.join("\n", configurationData.getCommentsForSection(property.getPath()));
                Arrays.stream(enumClass.getEnumConstants())
                    .filter(e -> !comments.contains(e.name()) && !isExcluded(property, e))
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

    private static boolean isExcluded(Property<?> property, Enum<?> enumValue) {
        return EXCLUDED_ENUMS.get(property).contains(Exclude.ALL)
            || EXCLUDED_ENUMS.get(property).contains(enumValue);
    }

    /**
     * Dummy enum to specify in the exclusion that all enum values
     * should be skipped. See its usages.
     */
    private enum Exclude {
        ALL
    }

}
