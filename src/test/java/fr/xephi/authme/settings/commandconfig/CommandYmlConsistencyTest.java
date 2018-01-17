package fr.xephi.authme.settings.commandconfig;

import ch.jalu.configme.configurationdata.ConfigurationDataBuilder;
import ch.jalu.configme.resource.PropertyResource;
import ch.jalu.configme.resource.YamlFileResource;
import fr.xephi.authme.TestHelper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests that commands.yml is well-formed.
 */
@RunWith(MockitoJUnitRunner.class)
public class CommandYmlConsistencyTest {

    @InjectMocks
    private CommandMigrationService commandMigrationService;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

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
