package fr.xephi.authme.settings.commandconfig;

import com.github.authme.configme.knownproperties.ConfigurationDataBuilder;
import com.github.authme.configme.resource.PropertyResource;
import com.github.authme.configme.resource.YamlFileResource;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.settings.SettingsMigrationService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

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

    @Before
    public void setUpSettingsMigrationService() {
        given(settingsMigrationService.getOnLoginCommands()).willReturn(Collections.emptyList());
        given(settingsMigrationService.getOnLoginConsoleCommands()).willReturn(Collections.emptyList());
        given(settingsMigrationService.getOnRegisterCommands()).willReturn(Collections.emptyList());
        given(settingsMigrationService.getOnRegisterConsoleCommands()).willReturn(Collections.emptyList());
    }

    @Test
    public void shouldLoadWithNoMigrations() {
        // given
        File commandFile = TestHelper.getJarFile("/commands.yml");
        PropertyResource resource = new YamlFileResource(commandFile);

        // when
        boolean result = commandMigrationService.checkAndMigrate(
            resource, ConfigurationDataBuilder.collectData(CommandSettingsHolder.class).getProperties());

        // then
        assertThat(result, equalTo(false));
    }
}
