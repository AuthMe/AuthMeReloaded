package fr.xephi.authme.settings.commandconfig;

import ch.jalu.configme.configurationdata.ConfigurationDataBuilder;
import ch.jalu.configme.resource.PropertyResource;
import ch.jalu.configme.resource.YamlFileResource;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.settings.SettingsMigrationService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Tests that commands.yml is well-formed.
 */
@RunWith(MockitoJUnitRunner.class)
public class CommandYmlConsistencyTest {

    @InjectMocks
    private CommandMigrationService commandMigrationService;

    @Mock
    private SettingsMigrationService settingsMigrationService;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldLoadWithNoMigrations() {
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
