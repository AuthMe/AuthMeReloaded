package fr.xephi.authme.settings;

import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.configurationdata.ConfigurationDataBuilder;
import ch.jalu.configme.migration.PlainMigrationService;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.resource.PropertyResource;
import ch.jalu.configme.resource.YamlFileResource;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.settings.properties.TestConfiguration;
import fr.xephi.authme.settings.properties.TestEnum;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static fr.xephi.authme.TestHelper.getJarFile;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Integration test for {@link Settings} (ConfigMe integration).
 */
public class SettingsIntegrationTest {

    /** File name of the sample config including all {@link TestConfiguration} values. */
    private static final String COMPLETE_FILE = TestHelper.PROJECT_ROOT + "settings/config-sample-values.yml";
    /** File name of the sample config missing certain {@link TestConfiguration} values. */
    private static final String INCOMPLETE_FILE = TestHelper.PROJECT_ROOT + "settings/config-incomplete-sample.yml";

    private static ConfigurationData CONFIG_DATA =
        ConfigurationDataBuilder.createConfiguration(TestConfiguration.class);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File testPluginFolder;

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Before
    public void setUpTestPluginFolder() throws IOException {
        testPluginFolder = temporaryFolder.newFolder();
    }

    @Test
    public void shouldLoadAndReadAllProperties() throws IOException {
        // given
        PropertyResource resource = new YamlFileResource(copyFileFromResources(COMPLETE_FILE));
        // Pass another, non-existent file to check if the settings had to be rewritten
        File newFile = temporaryFolder.newFile();

        // when / then
        Settings settings = new Settings(testPluginFolder, resource,
            new PlainMigrationService(), CONFIG_DATA);
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
        PropertyResource resource = new YamlFileResource(file);
        // Expectation: File is rewritten to since it does not have all configurations
        new Settings(testPluginFolder, resource, new PlainMigrationService(), CONFIG_DATA);

        // Load the settings again -> checks that what we wrote can be loaded again
        resource = new YamlFileResource(file);

        // then
        Settings settings = new Settings(testPluginFolder, resource,
            new PlainMigrationService(), CONFIG_DATA);
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

    @Test
    public void shouldReloadSettings() throws IOException {
        // given
        File configFile = temporaryFolder.newFile();
        PropertyResource resource = new YamlFileResource(configFile);
        Settings settings = new Settings(testPluginFolder, resource, null, CONFIG_DATA);

        // when
        assertThat(settings.getProperty(TestConfiguration.RATIO_ORDER), equalTo(TestEnum.SECOND)); // default value
        Files.copy(getJarFile(COMPLETE_FILE), configFile);
        settings.reload();

        // then
        assertThat(settings.getProperty(TestConfiguration.RATIO_ORDER), equalTo(TestEnum.FIRST));
    }

    private File copyFileFromResources(String path) {
        try {
            File source = getJarFile(path);
            File destination = temporaryFolder.newFile();
            Files.copy(source, destination);
            return destination;
        } catch (IOException e) {
            throw new IllegalStateException("Could not copy test file", e);
        }
    }

}
