package fr.xephi.authme.settings.commandconfig;

import ch.jalu.configme.beanmapper.BeanDescriptionFactory;
import ch.jalu.configme.beanmapper.BeanPropertyDescription;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static fr.xephi.authme.settings.commandconfig.CommandConfigTestHelper.isCommand;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link CommandMigrationService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CommandMigrationServiceTest {

    @InjectMocks
    private CommandMigrationService commandMigrationService;

    @Mock
    private SettingsMigrationService settingsMigrationService;

    private CommandConfig commandConfig = new CommandConfig();

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldNotPerformAnyMigration() {
        // given
        given(settingsMigrationService.getOnLoginCommands()).willReturn(emptyList());
        given(settingsMigrationService.getOnLoginConsoleCommands()).willReturn(emptyList());
        given(settingsMigrationService.getOnRegisterCommands()).willReturn(emptyList());
        given(settingsMigrationService.getOnRegisterConsoleCommands()).willReturn(emptyList());
        commandConfig.getOnRegister().put("existing", new Command("existing cmd", Executor.PLAYER));
        CommandConfig configSpy = spy(commandConfig);

        // when
        boolean result = commandMigrationService.transformOldCommands(configSpy);

        // then
        assertThat(result, equalTo(false));
        verifyZeroInteractions(configSpy);
        assertThat(configSpy.getOnRegister().keySet(), contains("existing"));
        assertThat(configSpy.getOnLogin(), anEmptyMap());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldPerformMigration() {
        // given
        List<String> onLogin = Collections.singletonList("on login command");
        given(settingsMigrationService.getOnLoginCommands()).willReturn(onLogin);
        List<String> onLoginConsole = Arrays.asList("cmd1", "cmd2 %p", "cmd3");
        given(settingsMigrationService.getOnLoginConsoleCommands()).willReturn(onLoginConsole);
        given(settingsMigrationService.getOnRegisterCommands()).willReturn(emptyList());
        List<String> onRegisterConsole = Arrays.asList("log %p registered", "whois %p");
        given(settingsMigrationService.getOnRegisterConsoleCommands()).willReturn(onRegisterConsole);

        Map<String, Command> onLoginCommands = new LinkedHashMap<>();
        onLoginCommands.put("bcast", new Command("bcast %p returned", Executor.CONSOLE));
        commandConfig.setOnLogin(onLoginCommands);
        Map<String, Command> onRegisterCommands = new LinkedHashMap<>();
        onRegisterCommands.put("ex_cmd", new Command("existing", Executor.CONSOLE));
        onRegisterCommands.put("ex_cmd2", new Command("existing2", Executor.PLAYER));
        commandConfig.setOnRegister(onRegisterCommands);

        // when
        boolean result = commandMigrationService.transformOldCommands(commandConfig);

        // then
        assertThat(result, equalTo(true));
        assertThat(commandConfig.getOnLogin(), sameInstance(onLoginCommands));
        Collection<Command> loginCmdList = onLoginCommands.values();
        assertThat(loginCmdList, contains(
            equalTo(onLoginCommands.get("bcast")),
            isCommand("on login command", Executor.PLAYER),
            isCommand("cmd1", Executor.CONSOLE),
            isCommand("cmd2 %p", Executor.CONSOLE),
            isCommand("cmd3", Executor.CONSOLE)));

        assertThat(commandConfig.getOnRegister(), sameInstance(onRegisterCommands));
        Collection<Command> registerCmdList = onRegisterCommands.values();
        assertThat(registerCmdList, contains(
            isCommand("existing", Executor.CONSOLE),
            isCommand("existing2", Executor.PLAYER),
            isCommand("log %p registered", Executor.CONSOLE),
            isCommand("whois %p", Executor.CONSOLE)));
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
