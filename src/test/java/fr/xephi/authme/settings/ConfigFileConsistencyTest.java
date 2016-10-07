package fr.xephi.authme.settings;

import com.github.authme.configme.knownproperties.PropertyEntry;
import com.github.authme.configme.migration.MigrationService;
import com.github.authme.configme.migration.PlainMigrationService;
import com.github.authme.configme.properties.Property;
import com.github.authme.configme.resource.PropertyResource;
import com.github.authme.configme.resource.YamlFileResource;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.settings.properties.AuthMeSettingsRetriever;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Test for {@link Settings} and the project's config.yml,
 * verifying that no settings are missing from the file.
 */
public class ConfigFileConsistencyTest {

    /** The file name of the project's sample config file. */
    private static final String CONFIG_FILE = "/config.yml";

    @Test
    public void shouldHaveAllConfigs() throws IOException {
        // given
        File configFile = TestHelper.getJarFile(CONFIG_FILE);
        PropertyResource resource = new YamlFileResource(configFile);
        MigrationService migration = new PlainMigrationService();

        // when
        boolean result = migration.checkAndMigrate(resource, AuthMeSettingsRetriever.getAllPropertyFields());

        // then
        if (result) {
            Set<String> knownProperties = getAllKnownPropertyPaths();
            List<String> missingProperties = new ArrayList<>();
            for (String path : knownProperties) {
                if (!resource.contains(path)) {
                    missingProperties.add(path);
                }
            }
            fail("Found missing properties!\n-" + String.join("\n-", missingProperties));
        }
    }

    @Test
    public void shouldNotHaveUnknownConfigs() {
        // given
        File configFile = TestHelper.getJarFile(CONFIG_FILE);
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
        Map<String, Object> allReadProperties = configuration.getValues(true);
        Set<String> knownKeys = getAllKnownPropertyPaths();

        // when
        List<String> unknownPaths = new ArrayList<>();
        for (Map.Entry<String, Object> entry : allReadProperties.entrySet()) {
            // The value being a MemorySection means it's a parent node
            if (!(entry.getValue() instanceof MemorySection) && !knownKeys.contains(entry.getKey())) {
                unknownPaths.add(entry.getKey());
            }
        }

        // then
        if (!unknownPaths.isEmpty()) {
            fail("Found " + unknownPaths.size() + " unknown property paths in the project's config.yml: \n- "
                + String.join("\n- ", unknownPaths));
        }
    }

    @Test
    public void shouldHaveValueCorrespondingToPropertyDefault() {
        // given
        File configFile = TestHelper.getJarFile(CONFIG_FILE);
        PropertyResource resource = new YamlFileResource(configFile);
        List<PropertyEntry> knownProperties = AuthMeSettingsRetriever.getAllPropertyFields();

        // when / then
        for (PropertyEntry propertyEntry : knownProperties) {
            Property<?> property = propertyEntry.getProperty();
            assertThat("Default value of '" + property.getPath() + "' in config.yml should be the same as in Property",
                property.getValue(resource).equals(property.getDefaultValue()), equalTo(true));
        }
    }

    private static Set<String> getAllKnownPropertyPaths() {
        List<PropertyEntry> knownProperties = AuthMeSettingsRetriever.getAllPropertyFields();
        Set<String> paths = new HashSet<>(knownProperties.size());
        for (PropertyEntry propertyEntry : knownProperties) {
            paths.add(propertyEntry.getProperty().getPath());
        }
        return paths;
    }

}
