package fr.xephi.authme.settings;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import fr.xephi.authme.ConsoleLoggerTestInitializer;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.properties.TestConfiguration;
import fr.xephi.authme.settings.properties.TestEnum;
import fr.xephi.authme.settings.propertymap.PropertyMap;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static fr.xephi.authme.settings.domain.Property.newProperty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Integration test for {@link NewSetting}.
 */
public class NewSettingIntegrationTest {

    /** File name of the sample config including all {@link TestConfiguration} values. */
    private static final String COMPLETE_FILE = "/config-sample-values.yml";
    /** File name of the sample config missing certain {@link TestConfiguration} values. */
    private static final String INCOMPLETE_FILE = "/config-incomplete-sample.yml";
    /** File name for testing difficult values. */
    private static final String DIFFICULT_FILE = "/config-difficult-values.yml";

    private static PropertyMap propertyMap = TestConfiguration.generatePropertyMap();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpLogger() {
        ConsoleLoggerTestInitializer.setupLogger();
    }

    @Test
    public void shouldLoadAndReadAllProperties() throws IOException {
        // given
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(copyFileFromResources(COMPLETE_FILE));
        // Pass another, non-existent file to check if the settings had to be rewritten
        File newFile = temporaryFolder.newFile();

        // when / then
        NewSetting settings = new NewSetting(configuration, newFile, propertyMap);
        Map<Property<?>, Object> expectedValues = ImmutableMap.<Property<?>, Object>builder()
            .put(TestConfiguration.DURATION_IN_SECONDS, 22)
            .put(TestConfiguration.SYSTEM_NAME, "Custom sys name")
            .put(TestConfiguration.RATIO_ORDER, TestEnum.FIRST)
            .put(TestConfiguration.RATIO_FIELDS, Arrays.asList("Australia", "Burundi", "Colombia"))
            .put(TestConfiguration.VERSION_NUMBER, 2492)
            .put(TestConfiguration.SKIP_BORING_FEATURES, false)
            .put(TestConfiguration.BORING_COLORS, Arrays.asList("beige", "gray"))
            .put(TestConfiguration.DUST_LEVEL, 2)
            .put(TestConfiguration.USE_COOL_FEATURES, true)
            .put(TestConfiguration.COOL_OPTIONS, Arrays.asList("Dinosaurs", "Explosions", "Big trucks"))
            .build();
        for (Map.Entry<Property<?>, Object> entry : expectedValues.entrySet()) {
            assertThat("Property '" + entry.getKey().getPath() + "' has expected value",
                settings.getProperty(entry.getKey()), equalTo(entry.getValue()));
        }
        assertThat(newFile.length(), equalTo(0L));
    }

    @Test
    public void shouldWriteMissingProperties() {
        // given/when
        File file = copyFileFromResources(INCOMPLETE_FILE);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        // Expectation: File is rewritten to since it does not have all configurations
        new NewSetting(configuration, file, propertyMap);

        // Load the settings again -> checks that what we wrote can be loaded again
        configuration = YamlConfiguration.loadConfiguration(file);

        // then
        NewSetting settings = new NewSetting(configuration, file, propertyMap);
        Map<Property<?>, Object> expectedValues = ImmutableMap.<Property<?>, Object>builder()
            .put(TestConfiguration.DURATION_IN_SECONDS, 22)
            .put(TestConfiguration.SYSTEM_NAME, "[TestDefaultValue]")
            .put(TestConfiguration.RATIO_ORDER, TestEnum.SECOND)
            .put(TestConfiguration.RATIO_FIELDS, Arrays.asList("Australia", "Burundi", "Colombia"))
            .put(TestConfiguration.VERSION_NUMBER, 32046)
            .put(TestConfiguration.SKIP_BORING_FEATURES, false)
            .put(TestConfiguration.BORING_COLORS, Collections.EMPTY_LIST)
            .put(TestConfiguration.DUST_LEVEL, -1)
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
        File file = copyFileFromResources(DIFFICULT_FILE);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);

        // Additional string properties
        List<Property<String>> additionalProperties = Arrays.asList(
            newProperty("more.string1", "it's a text with some \\'apostrophes'"),
            newProperty("more.string2", "\tthis one\nhas some\nnew '' lines-test")
        );
        PropertyMap propertyMap = TestConfiguration.generatePropertyMap();
        for (Property<?> property : additionalProperties) {
            propertyMap.put(property, new String[0]);
        }

        // when
        new NewSetting(configuration, file, propertyMap);
        // reload the file as settings should have been rewritten
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
            .put(TestConfiguration.RATIO_ORDER, TestEnum.FOURTH)
            .put(TestConfiguration.RATIO_FIELDS, Arrays.asList("Australia\\", "\tBurundi'", "Colombia?\n''"))
            .put(TestConfiguration.VERSION_NUMBER, -1337)
            .put(TestConfiguration.SKIP_BORING_FEATURES, false)
            .put(TestConfiguration.BORING_COLORS, Arrays.asList("it's a difficult string!", "gray\nwith new lines\n"))
            .put(TestConfiguration.DUST_LEVEL, -1)
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

    @Test
    @Ignore
    // TODO #603: Un-ignore once migration service is passed to settings
    public void shouldReloadSettings() throws IOException {
        // given
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(temporaryFolder.newFile());
        File fullConfigFile = copyFileFromResources(COMPLETE_FILE);
        NewSetting settings = new NewSetting(configuration, fullConfigFile, null);

        // when
        assertThat(settings.getProperty(TestConfiguration.RATIO_ORDER),
            equalTo(TestConfiguration.RATIO_ORDER.getDefaultValue()));
        settings.reload();

        // then
        assertThat(settings.getProperty(TestConfiguration.RATIO_ORDER), equalTo(TestEnum.FIRST));
    }

    private File copyFileFromResources(String path) {
        try {
            File source = TestHelper.getJarFile(path);
            File destination = temporaryFolder.newFile();
            Files.copy(source, destination);
            return destination;
        } catch (IOException e) {
            throw new IllegalStateException("Could not copy test file", e);
        }
    }

}
