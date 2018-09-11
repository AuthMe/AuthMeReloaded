package fr.xephi.authme.settings.commandconfig;

import ch.jalu.configme.beanmapper.propertydescription.BeanDescriptionFactoryImpl;
import ch.jalu.configme.beanmapper.propertydescription.BeanPropertyDescription;
import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.configurationdata.ConfigurationDataBuilder;
import ch.jalu.configme.resource.PropertyResource;
import ch.jalu.configme.resource.YamlFileResource;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.settings.SettingsMigrationService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Test for {@link CommandMigrationService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CommandMigrationServiceTest {

    @InjectMocks
    private CommandMigrationService commandMigrationService;

    @Mock
    private SettingsMigrationService settingsMigrationService;

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
            resource.createReader(), ConfigurationDataBuilder.createConfiguration(CommandSettingsHolder.class));

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
            resource.createReader(), ConfigurationDataBuilder.createConfiguration(CommandSettingsHolder.class));

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
            resource.createReader(), ConfigurationDataBuilder.createConfiguration(CommandSettingsHolder.class));

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
        String[] properties = new BeanDescriptionFactoryImpl()
            .getAllProperties(CommandConfig.class)
            .stream()
            .map(BeanPropertyDescription::getName)
            .toArray(String[]::new);

        // when / then
        assertThat(CommandMigrationService.COMMAND_CONFIG_PROPERTIES, containsInAnyOrder(properties));
    }

    @Test
    public void shouldMigrateOldOtherAccountsCommand() {
        // given
        given(settingsMigrationService.hasOldOtherAccountsCommand()).willReturn(true);
        given(settingsMigrationService.getOldOtherAccountsCommand())
            .willReturn("helpop %playername% (%playerip%) has other accounts!");
        given(settingsMigrationService.getOldOtherAccountsCommandThreshold()).willReturn(3);
        File commandFile = TestHelper.getJarFile(TestHelper.PROJECT_ROOT + "settings/commandconfig/commands.complete.yml");
        PropertyResource resource = new YamlFileResource(commandFile);
        ConfigurationData configurationData = ConfigurationDataBuilder.createConfiguration(CommandSettingsHolder.class);

        // when
        commandMigrationService.checkAndMigrate(
            resource.createReader(), configurationData);

        // then
        Map<String, OnLoginCommand> onLoginCommands = configurationData.getValue(CommandSettingsHolder.COMMANDS).getOnLogin();
        assertThat(onLoginCommands, aMapWithSize(6)); // 5 in the file + the newly migrated on
        OnLoginCommand newCommand = getUnknownOnLoginCommand(onLoginCommands);
        assertThat(newCommand.getCommand(), equalTo("helpop %p (%ip) has other accounts!"));
        assertThat(newCommand.getExecutor(), equalTo(Executor.CONSOLE));
        assertThat(newCommand.getIfNumberOfAccountsAtLeast().get(), equalTo(3));
        assertThat(newCommand.getIfNumberOfAccountsLessThan().isPresent(), equalTo(false));
    }

    /*
     * Returns the command under onLogin from commands.complete.yml that isn't present in the beginning.
     */
    private static OnLoginCommand getUnknownOnLoginCommand(Map<String, OnLoginCommand> onLoginCommands) {
        Set<String> knownKeys = newHashSet("welcome", "show_motd", "display_list", "warn_for_alts", "log_suspicious_user");
        List<String> unknownKeys = onLoginCommands.keySet().stream()
            .filter(key -> !knownKeys.contains(key))
            .collect(Collectors.toList());
        if (unknownKeys.size() == 1) {
            return onLoginCommands.get(unknownKeys.get(0));
        } else {
            throw new IllegalStateException("Expected 1 unknown key but found " + unknownKeys.size() + ": " + unknownKeys);
        }
    }
}
