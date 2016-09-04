package fr.xephi.authme.settings;

import com.github.authme.configme.knownproperties.PropertyEntry;
import com.github.authme.configme.resource.PropertyResource;
import com.github.authme.configme.resource.YamlFileResource;
import com.google.common.io.Files;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.settings.properties.AuthMeSettingsRetriever;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

/**
 * Test for {@link SettingsMigrationService}.
 */
public class SettingsMigrationServiceTest {

    @Rule
    public TemporaryFolder testFolderHandler = new TemporaryFolder();

    private File testFolder;
    private File configTestFile;

    /**
     * Ensure that AuthMe regards the JAR's own config.yml as complete.
     * If something legitimately needs migrating, a test from {@link ConfigFileConsistencyTest} should fail.
     * If none fails in that class, it means something is wrong with the migration service
     * as it wants to perform a migration on our up-to-date config.yml.
     */
    @Test
    public void shouldNotRewriteJarConfig() throws IOException {
        // given
        copyConfigToTestFolder();
        PropertyResource resource = new YamlFileResource(configTestFile);
        List<PropertyEntry> propertyMap = AuthMeSettingsRetriever.getAllPropertyFields();
        assumeThat(testFolder.listFiles(), arrayWithSize(1));
        SettingsMigrationService migrationService = new SettingsMigrationService(testFolder);

        // when
        boolean result = migrationService.checkAndMigrate(resource, propertyMap);

        // then
        assertThat(result, equalTo(false));
        assertThat(testFolder.listFiles(), arrayWithSize(1));
    }

    private void copyConfigToTestFolder() throws IOException {
        testFolder = testFolderHandler.newFolder("migrationtest");

        final File testConfig = testFolderHandler.newFile("migrationtest/config.yml");
        final File realConfig = TestHelper.getJarFile("/config.yml");

        Files.copy(realConfig, testConfig);
        if (!testConfig.exists()) {
            throw new IOException("Could not copy project's config.yml to test folder");
        }
        configTestFile = testConfig;
    }
}
