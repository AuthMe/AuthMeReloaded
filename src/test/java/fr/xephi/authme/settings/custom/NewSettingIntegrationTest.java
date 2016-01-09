package fr.xephi.authme.settings.custom;

import com.google.common.collect.ImmutableMap;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.propertymap.PropertyMap;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

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

    private static PropertyMap propertyMap;

    @BeforeClass
    public static void generatePropertyMap() {
        propertyMap = new PropertyMap();
        for (Field field : TestConfiguration.class.getDeclaredFields()) {
            Object fieldValue = ReflectionTestUtils.getFieldValue(TestConfiguration.class, null, field.getName());
            if (fieldValue instanceof Property<?>) {
                Property<?> property = (Property<?>) fieldValue;
                String[] comments = new String[]{"Comment for '" + property.getPath() + "'"};
                propertyMap.put(property, comments);
            }
        }
    }

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

    private File getConfigFile(String file) {
        URL url = getClass().getClassLoader().getResource(file);
        if (url == null) {
            throw new IllegalStateException("File '" + file + "' could not be loaded");
        }
        return new File(url.getFile());
    }

}
