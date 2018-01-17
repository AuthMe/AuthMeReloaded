package fr.xephi.authme.settings.commandconfig;

import ch.jalu.configme.beanmapper.BeanDescriptionFactory;
import ch.jalu.configme.beanmapper.BeanPropertyDescription;
import ch.jalu.configme.configurationdata.ConfigurationDataBuilder;
import ch.jalu.configme.resource.PropertyResource;
import ch.jalu.configme.resource.YamlFileResource;
import fr.xephi.authme.TestHelper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link CommandMigrationService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CommandMigrationServiceTest {

    @InjectMocks
    private CommandMigrationService commandMigrationService;

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldRewriteForEmptyFile() {
        // given
        File commandFile = TestHelper.getJarFile(TestHelper.PROJECT_ROOT + "settings/commandconfig/commands.empty.yml");
        PropertyResource resource = new YamlFileResource(commandFile);

        // when
        boolean result = commandMigrationService.checkAndMigrate(
            resource, ConfigurationDataBuilder.collectData(CommandSettingsHolder.class).getProperties());

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldRewriteIncompleteFile() {
        // given
        File commandFile = TestHelper.getJarFile(TestHelper.PROJECT_ROOT + "settings/commandconfig/commands.incomplete.yml");
        PropertyResource resource = new YamlFileResource(commandFile);

        // when
        boolean result = commandMigrationService.checkAndMigrate(
            resource, ConfigurationDataBuilder.collectData(CommandSettingsHolder.class).getProperties());

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldNotChangeCompleteFile() {
        // given
        File commandFile = TestHelper.getJarFile(TestHelper.PROJECT_ROOT + "settings/commandconfig/commands.complete.yml");
        PropertyResource resource = new YamlFileResource(commandFile);

        // when
        boolean result = commandMigrationService.checkAndMigrate(
            resource, ConfigurationDataBuilder.collectData(CommandSettingsHolder.class).getProperties());

        // then
        assertThat(result, equalTo(false));
    }

    /**
     * Checks that {@link CommandMigrationService#COMMAND_CONFIG_PROPERTIES} contains all properties defined in the
     * {@link CommandConfig} class. It is used to ensure that the commands.yml file is complete.
     */
    @Test
    public void shouldHaveAllPropertiesFromCommandConfig() {
        // given
        String[] properties = new BeanDescriptionFactory()
            .collectWritableFields(CommandConfig.class)
            .stream()
            .map(BeanPropertyDescription::getName)
            .toArray(String[]::new);

        // when / then
        assertThat(CommandMigrationService.COMMAND_CONFIG_PROPERTIES, containsInAnyOrder(properties));
    }
}
