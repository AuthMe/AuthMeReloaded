package fr.xephi.authme.settings;

import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.resource.PropertyReader;
import ch.jalu.configme.resource.PropertyResource;
import ch.jalu.configme.resource.YamlFileResource;
import com.google.common.io.Files;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.output.LogLevel;
import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.settings.properties.AuthMeSettingsRetriever;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static fr.xephi.authme.TestHelper.getJarFile;
import static fr.xephi.authme.settings.properties.DatabaseSettings.MYSQL_COL_SALT;
import static fr.xephi.authme.settings.properties.PluginSettings.ENABLE_PERMISSION_CHECK;
import static fr.xephi.authme.settings.properties.PluginSettings.LOG_LEVEL;
import static fr.xephi.authme.settings.properties.PluginSettings.REGISTERED_GROUP;
import static fr.xephi.authme.settings.properties.PluginSettings.UNREGISTERED_GROUP;
import static fr.xephi.authme.settings.properties.RegistrationSettings.DELAY_JOIN_MESSAGE;
import static fr.xephi.authme.settings.properties.RegistrationSettings.REGISTER_SECOND_ARGUMENT;
import static fr.xephi.authme.settings.properties.RegistrationSettings.REGISTRATION_TYPE;
import static fr.xephi.authme.settings.properties.RestrictionSettings.ALLOWED_NICKNAME_CHARACTERS;
import static fr.xephi.authme.settings.properties.RestrictionSettings.FORCE_SPAWN_LOCATION_AFTER_LOGIN;
import static fr.xephi.authme.settings.properties.RestrictionSettings.FORCE_SPAWN_ON_WORLDS;
import static fr.xephi.authme.settings.properties.SecuritySettings.LEGACY_HASHES;
import static fr.xephi.authme.settings.properties.SecuritySettings.PASSWORD_HASH;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link SettingsMigrationService}.
 */
public class SettingsMigrationServiceTest {

    private static final String OLD_CONFIG_FILE = TestHelper.PROJECT_ROOT + "settings/config-old.yml";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    /* When settings are loaded, test that migrations are applied and immediately available in memory. */
    @Test
    public void shouldPerformMigrationsInMemory() throws IOException {
        // given
        File dataFolder = temporaryFolder.newFolder();
        File configFile = new File(dataFolder, "config.yml");
        Files.copy(getJarFile(OLD_CONFIG_FILE), configFile);
        PropertyResource resource = new YamlFileResource(configFile);
        SettingsMigrationService migrationService = new SettingsMigrationService(dataFolder);

        // when
        Settings settings = new Settings(
            dataFolder, resource, migrationService, AuthMeSettingsRetriever.buildConfigurationData());

        // then
        verifyHasUpToDateSettings(settings, dataFolder);
    }

    /*
     * When settings are loaded, test that migrations are applied and persisted to disk,
     * i.e. when the settings are loaded again from the file, no migrations should be necessary.
     */
    @Test
    public void shouldPerformMigrationsAndPersistToDisk() throws IOException {
        // given
        File dataFolder = temporaryFolder.newFolder();
        File configFile = new File(dataFolder, "config.yml");
        Files.copy(getJarFile(OLD_CONFIG_FILE), configFile);
        PropertyResource resource = new YamlFileResource(configFile);
        TestMigrationServiceExtension migrationService = new TestMigrationServiceExtension(dataFolder);
        ConfigurationData configurationData = AuthMeSettingsRetriever.buildConfigurationData();

        // when
        new Settings(dataFolder, resource, migrationService, configurationData);
        resource = new YamlFileResource(configFile);
        Settings settings = new Settings(dataFolder, resource, migrationService, configurationData);

        // then
        verifyHasUpToDateSettings(settings, dataFolder);
        assertThat(migrationService.returnedValues, contains(true, false));
    }

    @Test
    public void shouldKeepOldOtherAccountsSettings() throws IOException {
        // given
        File dataFolder = temporaryFolder.newFolder();
        File configFile = new File(dataFolder, "config.yml");
        Files.copy(getJarFile(OLD_CONFIG_FILE), configFile);
        PropertyResource resource = new YamlFileResource(configFile);
        SettingsMigrationService migrationService = new SettingsMigrationService(dataFolder);

        // when
        migrationService.performMigrations(resource.createReader(), AuthMeSettingsRetriever.buildConfigurationData());

        // then
        assertThat(migrationService.hasOldOtherAccountsCommand(), equalTo(true));
        assertThat(migrationService.getOldOtherAccountsCommand(), equalTo("msg admin %playername% has a lot of accounts!"));
        assertThat(migrationService.getOldOtherAccountsCommandThreshold(), equalTo(5));
    }

    private void verifyHasUpToDateSettings(Settings settings, File dataFolder) throws IOException {
        assertThat(settings.getProperty(ALLOWED_NICKNAME_CHARACTERS), equalTo(ALLOWED_NICKNAME_CHARACTERS.getDefaultValue()));
        assertThat(settings.getProperty(DELAY_JOIN_MESSAGE), equalTo(true));
        assertThat(settings.getProperty(FORCE_SPAWN_LOCATION_AFTER_LOGIN), equalTo(true));
        assertThat(settings.getProperty(FORCE_SPAWN_ON_WORLDS), contains("survival", "survival_nether", "creative"));
        assertThat(settings.getProperty(LOG_LEVEL), equalTo(LogLevel.INFO));
        assertThat(settings.getProperty(REGISTRATION_TYPE), equalTo(RegistrationType.EMAIL));
        assertThat(settings.getProperty(REGISTER_SECOND_ARGUMENT), equalTo(RegisterSecondaryArgument.CONFIRMATION));
        assertThat(settings.getProperty(ENABLE_PERMISSION_CHECK), equalTo(true));
        assertThat(settings.getProperty(REGISTERED_GROUP), equalTo("unLoggedinGroup"));
        assertThat(settings.getProperty(UNREGISTERED_GROUP), equalTo(""));
        assertThat(settings.getProperty(PASSWORD_HASH), equalTo(HashAlgorithm.SHA256));
        assertThat(settings.getProperty(LEGACY_HASHES), contains(HashAlgorithm.PBKDF2, HashAlgorithm.WORDPRESS, HashAlgorithm.SHA512));
        assertThat(settings.getProperty(MYSQL_COL_SALT), equalTo("salt_col_name"));

        // Check migration of old setting to email.html
        assertThat(Files.readLines(new File(dataFolder, "email.html"), StandardCharsets.UTF_8),
            contains("Dear <playername />, <br /><br /> This is your new AuthMe password for the server "
                + "<br /><br /> <servername /> : <br /><br /> <generatedpass /><br /><image /><br />Do not forget to "
                + "change password after login! <br /> /changepassword <generatedpass /> newPassword"));
    }

    private static class TestMigrationServiceExtension extends SettingsMigrationService {
        private List<Boolean> returnedValues = new ArrayList<>();

        TestMigrationServiceExtension(@DataFolder File pluginFolder) {
            super(pluginFolder);
        }

        @Override
        protected boolean performMigrations(PropertyReader reader, ConfigurationData configurationData) {
            boolean result = super.performMigrations(reader, configurationData);
            returnedValues.add(result);
            return result;
        }
    }
}
