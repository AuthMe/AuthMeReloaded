package fr.xephi.authme.settings;

import com.google.common.collect.ImmutableMap;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.properties.TestConfiguration;
import fr.xephi.authme.settings.propertymap.PropertyMap;
import fr.xephi.authme.util.WrapperMock;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static fr.xephi.authme.settings.domain.Property.newProperty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

/**
 * Integration test for {@link NewSetting}.
 */
public class NewSettingIntegrationTest {

    /** File name of the sample config including all {@link TestConfiguration} values. */
    private static final String COMPLETE_FILE = "config-sample-values.yml";
    /** File name of the sample config missing certain {@link TestConfiguration} values. */
    private static final String INCOMPLETE_FILE = "config-incomplete-sample.yml";
    /** File name for testing difficult values. */
    private static final String DIFFICULT_FILE = "config-difficult-values.yml";

    private static PropertyMap propertyMap = generatePropertyMap();

    @Test
    public void shouldLoadAndReadAllProperties() {
        // given
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(getConfigFile(COMPLETE_FILE));
        File file = new File("unused");
        assumeThat(file.exists(), equalTo(false));

        // when / then
        NewSetting settings = new NewSetting(configuration, file, propertyMap);
        Map<Property<?>, Object> expectedValues = ImmutableMap.<Property<?>, Object>builder()
            .put(TestConfiguration.DURATION_IN_SECONDS, 22)
            .put(TestConfiguration.SYSTEM_NAME, "Custom sys name")
            .put(TestConfiguration.RATIO_LIMIT, -4.1)
            .put(TestConfiguration.RATIO_FIELDS, Arrays.asList("Australia", "Burundi", "Colombia"))
            .put(TestConfiguration.VERSION_NUMBER, 2492)
            .put(TestConfiguration.SKIP_BORING_FEATURES, false)
            .put(TestConfiguration.BORING_COLORS, Arrays.asList("beige", "gray"))
            .put(TestConfiguration.DUST_LEVEL, 0.81)
            .put(TestConfiguration.USE_COOL_FEATURES, true)
            .put(TestConfiguration.COOL_OPTIONS, Arrays.asList("Dinosaurs", "Explosions", "Big trucks"))
            .build();
        for (Map.Entry<Property<?>, Object> entry : expectedValues.entrySet()) {
            assertThat("Property '" + entry.getKey().getPath() + "' has expected value",
                settings.getProperty(entry.getKey()), equalTo(entry.getValue()));
        }
        assertThat(file.exists(), equalTo(false));
    }

    @Test
    public void shouldWriteMissingProperties() {
        // given/when
        File file = getConfigFile(INCOMPLETE_FILE);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        assumeThat(configuration.contains(TestConfiguration.BORING_COLORS.getPath()), equalTo(false));
        // Expectation: File is rewritten to since it does not have all configurations
        new NewSetting(configuration, file, propertyMap);

        // Load the settings again -> checks that what we wrote can be loaded again
        configuration = YamlConfiguration.loadConfiguration(file);

        // then
        NewSetting settings = new NewSetting(configuration, file, propertyMap);
        Map<Property<?>, Object> expectedValues = ImmutableMap.<Property<?>, Object>builder()
            .put(TestConfiguration.DURATION_IN_SECONDS, 22)
            .put(TestConfiguration.SYSTEM_NAME, "[TestDefaultValue]")
            .put(TestConfiguration.RATIO_LIMIT, 3.0)
            .put(TestConfiguration.RATIO_FIELDS, Arrays.asList("Australia", "Burundi", "Colombia"))
            .put(TestConfiguration.VERSION_NUMBER, 32046)
            .put(TestConfiguration.SKIP_BORING_FEATURES, false)
            .put(TestConfiguration.BORING_COLORS, Collections.EMPTY_LIST)
            .put(TestConfiguration.DUST_LEVEL, 0.2)
            .put(TestConfiguration.USE_COOL_FEATURES, false)
            .put(TestConfiguration.COOL_OPTIONS, Arrays.asList("Dinosaurs", "Explosions", "Big trucks"))
            .build();
        for (Map.Entry<Property<?>, Object> entry : expectedValues.entrySet()) {
            assertThat("Property '" + entry.getKey().getPath() + "' has expected value",
                settings.getProperty(entry.getKey()), equalTo(entry.getValue()));
        }
    }

    /** Verify that "difficult cases" such as apostrophes in strings etc. are handled properly. */
    @Test
    public void shouldProperlyExportAnyValues() {
        // given
        File file = getConfigFile(DIFFICULT_FILE);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        assumeThat(configuration.contains(TestConfiguration.DUST_LEVEL.getPath()), equalTo(false));

        // Additional string properties
        List<Property<String>> additionalProperties = Arrays.asList(
            newProperty("more.string1", "it's a text with some \\'apostrophes'"),
            newProperty("more.string2", "\tthis one\nhas some\nnew '' lines-test")
        );
        PropertyMap propertyMap = generatePropertyMap();
        for (Property<?> property : additionalProperties) {
            propertyMap.put(property, new String[0]);
        }

        // when
        new NewSetting(configuration, file, propertyMap);
        // reload the file as settings should hav been rewritten
        configuration = YamlConfiguration.loadConfiguration(file);

        // then
        // assert that we won't rewrite the settings again! One rewrite should produce a valid, complete configuration
        File unusedFile = new File("config-difficult-values.unused.yml");
        NewSetting settings = new NewSetting(configuration, unusedFile, propertyMap);
        assertThat(unusedFile.exists(), equalTo(false));
        assertThat(configuration.contains(TestConfiguration.DUST_LEVEL.getPath()), equalTo(true));

        Map<Property<?>, Object> expectedValues = ImmutableMap.<Property<?>, Object>builder()
            .put(TestConfiguration.DURATION_IN_SECONDS, 20)
            .put(TestConfiguration.SYSTEM_NAME, "A 'test' name")
            .put(TestConfiguration.RATIO_LIMIT, -41.8)
            .put(TestConfiguration.RATIO_FIELDS, Arrays.asList("Australia\\", "\tBurundi'", "Colombia?\n''"))
            .put(TestConfiguration.VERSION_NUMBER, -1337)
            .put(TestConfiguration.SKIP_BORING_FEATURES, false)
            .put(TestConfiguration.BORING_COLORS, Arrays.asList("it's a difficult string!", "gray\nwith new lines\n"))
            .put(TestConfiguration.DUST_LEVEL, 0.2)
            .put(TestConfiguration.USE_COOL_FEATURES, true)
            .put(TestConfiguration.COOL_OPTIONS, Collections.EMPTY_LIST)
            .put(additionalProperties.get(0), additionalProperties.get(0).getDefaultValue())
            .put(additionalProperties.get(1), additionalProperties.get(1).getDefaultValue())
            .build();
        for (Map.Entry<Property<?>, Object> entry : expectedValues.entrySet()) {
            assertThat("Property '" + entry.getKey().getPath() + "' has expected value"
                + entry.getValue() + " but found " + settings.getProperty(entry.getKey()),
                settings.getProperty(entry.getKey()), equalTo(entry.getValue()));
        }
    }

    /**
     * Return a {@link File} instance to an existing file in the target/test-classes folder.
     *
     * @return The generated File
     */
    private File getConfigFile(String file) {
        URL url = getClass().getClassLoader().getResource(file);
        if (url == null) {
            throw new IllegalStateException("File '" + file + "' could not be loaded");
        }
        return new File(url.getFile());
    }

    /**
     * Generate a property map with all properties in {@link TestConfiguration}.
     *
     * @return The generated property map
     */
    private static PropertyMap generatePropertyMap() {
        WrapperMock.createInstance();
        PropertyMap propertyMap = new PropertyMap();
        for (Field field : TestConfiguration.class.getDeclaredFields()) {
            Object fieldValue = ReflectionTestUtils.getFieldValue(TestConfiguration.class, null, field.getName());
            if (fieldValue instanceof Property<?>) {
                Property<?> property = (Property<?>) fieldValue;
                String[] comments = new String[]{"Comment for '" + property.getPath() + "'"};
                propertyMap.put(property, comments);
            }
        }
        return propertyMap;
    }

}
