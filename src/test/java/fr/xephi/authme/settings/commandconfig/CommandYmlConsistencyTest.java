package fr.xephi.authme.settings.commandconfig;

import ch.jalu.configme.configurationdata.ConfigurationDataBuilder;
import ch.jalu.configme.resource.PropertyResource;
import ch.jalu.configme.resource.YamlFileResource;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.settings.SettingsMigrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;

/**
 * Tests that commands.yml is well-formed.
 */
@ExtendWith(MockitoExtension.class)
class CommandYmlConsistencyTest {

    @InjectMocks
    private CommandMigrationService commandMigrationService;

    @Mock
    private SettingsMigrationService settingsMigrationService;

    @Test
    void shouldLoadWithNoMigrations() {
        // given
        File commandFile = TestHelper.getJarFile("/commands.yml");
        PropertyResource resource = new YamlFileResource(commandFile);

        // when
        boolean result = commandMigrationService.checkAndMigrate(
            resource.createReader(), ConfigurationDataBuilder.createConfiguration(CommandSettingsHolder.class));

        // then
        assertThat(result, equalTo(false));
        verify(settingsMigrationService).hasOldOtherAccountsCommand();
    }
}
