package fr.xephi.authme.settings;

import com.github.authme.configme.resource.PropertyResource;
import com.github.authme.configme.resource.YamlFileResource;
import com.google.common.io.Files;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.output.LogLevel;
import fr.xephi.authme.settings.properties.AuthMeSettingsRetriever;
import fr.xephi.authme.settings.properties.RegistrationArgumentType;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static fr.xephi.authme.TestHelper.getJarFile;
import static fr.xephi.authme.settings.properties.PluginSettings.LOG_LEVEL;
import static fr.xephi.authme.settings.properties.RegistrationSettings.DELAY_JOIN_MESSAGE;
import static fr.xephi.authme.settings.properties.RegistrationSettings.REGISTRATION_TYPE;
import static fr.xephi.authme.settings.properties.RestrictionSettings.ALLOWED_NICKNAME_CHARACTERS;
import static fr.xephi.authme.settings.properties.RestrictionSettings.FORCE_SPAWN_LOCATION_AFTER_LOGIN;
import static fr.xephi.authme.settings.properties.RestrictionSettings.FORCE_SPAWN_ON_WORLDS;
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

    @Test
    public void shouldPerformMigrations() throws IOException {
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
        assertThat(settings.getProperty(ALLOWED_NICKNAME_CHARACTERS), equalTo(ALLOWED_NICKNAME_CHARACTERS.getDefaultValue()));
        assertThat(settings.getProperty(DELAY_JOIN_MESSAGE), equalTo(true));
        assertThat(settings.getProperty(FORCE_SPAWN_LOCATION_AFTER_LOGIN), equalTo(true));
        assertThat(settings.getProperty(FORCE_SPAWN_ON_WORLDS), contains("survival", "survival_nether", "creative"));
        assertThat(settings.getProperty(LOG_LEVEL), equalTo(LogLevel.INFO));
        assertThat(settings.getProperty(REGISTRATION_TYPE), equalTo(RegistrationArgumentType.EMAIL_WITH_CONFIRMATION));

        // Check migration of old setting to email.html
        assertThat(Files.readLines(new File(dataFolder, "email.html"), StandardCharsets.UTF_8),
            contains("Dear <playername />, <br /><br /> This is your new AuthMe password for the server "
                + "<br /><br /> <servername /> : <br /><br /> <generatedpass /><br /><image /><br />Do not forget to "
                + "change password after login! <br /> /changepassword <generatedpass /> newPassword"));
    }

    @Test
    public void shouldKeepOldForceCommandSettings() throws IOException {
        // given
        File dataFolder = temporaryFolder.newFolder();
        File configFile = new File(dataFolder, "config.yml");
        Files.copy(getJarFile(OLD_CONFIG_FILE), configFile);
        PropertyResource resource = new YamlFileResource(configFile);
        SettingsMigrationService migrationService = new SettingsMigrationService(dataFolder);

        // when
        migrationService.performMigrations(resource, AuthMeSettingsRetriever.buildConfigurationData().getProperties());

        // then
        assertThat(migrationService.getOnLoginCommands(), contains("spawn"));
        assertThat(migrationService.getOnLoginConsoleCommands(), contains("sethome %p:lastloc", "msg %p Welcome back"));
        assertThat(migrationService.getOnRegisterCommands(), contains("me registers", "msg CONSOLE hi"));
        assertThat(migrationService.getOnRegisterConsoleCommands(), contains("sethome %p:regloc"));
    }
}
