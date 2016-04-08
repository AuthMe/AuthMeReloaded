package fr.xephi.authme.command;

import fr.xephi.authme.AntiBot;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.help.HelpProvider;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.hooks.PluginHooks;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.ValidationService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link CommandService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CommandServiceTest {

    private CommandService commandService;
    @Mock
    private AuthMe authMe;
    @Mock
    private CommandMapper commandMapper;
    @Mock
    private HelpProvider helpProvider;
    @Mock
    private Messages messages;
    @Mock
    private PasswordSecurity passwordSecurity;
    @Mock
    private PermissionsManager permissionsManager;
    @Mock
    private NewSetting settings;
    @Mock
    private PluginHooks pluginHooks;
    @Mock
    private SpawnLoader spawnLoader;
    @Mock
    private AntiBot antiBot;
    @Mock
    private ValidationService validationService;
    @Mock
    private BukkitService bukkitService;

    @Before
    public void setUpService() {
        commandService = new CommandService(authMe, commandMapper, helpProvider, messages, passwordSecurity,
            permissionsManager, settings, pluginHooks, spawnLoader, antiBot, validationService, bukkitService);
    }

    @Test
    public void shouldSendMessage() {
        // given
        CommandSender sender = mock(CommandSender.class);

        // when
        commandService.send(sender, MessageKey.INVALID_EMAIL);

        // then
        verify(messages).send(sender, MessageKey.INVALID_EMAIL);
    }

    @Test
    public void shouldSendMessageWithReplacements() {
        // given
        CommandSender sender = mock(Player.class);

        // when
        commandService.send(sender, MessageKey.ANTIBOT_AUTO_ENABLED_MESSAGE, "10");

        // then
        verify(messages).send(sender, MessageKey.ANTIBOT_AUTO_ENABLED_MESSAGE, "10");
    }

    @Test
    public void shouldMapPartsToCommand() {
        // given
        CommandSender sender = mock(Player.class);
        List<String> commandParts = Arrays.asList("authme", "test", "test2");
        FoundCommandResult givenResult = mock(FoundCommandResult.class);
        given(commandMapper.mapPartsToCommand(sender, commandParts)).willReturn(givenResult);

        // when
        FoundCommandResult result = commandService.mapPartsToCommand(sender, commandParts);

        // then
        assertThat(result, equalTo(givenResult));
        verify(commandMapper).mapPartsToCommand(sender, commandParts);
    }

    @Test
    public void shouldGetDataSource() {
        // given
        DataSource dataSource = mock(DataSource.class);
        given(authMe.getDataSource()).willReturn(dataSource);

        // when
        DataSource result = commandService.getDataSource();

        // then
        assertThat(result, equalTo(dataSource));
    }

    @Test
    public void shouldGetPasswordSecurity() {
        // given/when
        PasswordSecurity passwordSecurity = commandService.getPasswordSecurity();

        // then
        assertThat(passwordSecurity, equalTo(this.passwordSecurity));
    }

    @Test
    public void shouldOutputHelp() {
        // given
        CommandSender sender = mock(CommandSender.class);
        FoundCommandResult result = mock(FoundCommandResult.class);
        int options = HelpProvider.SHOW_LONG_DESCRIPTION;
        List<String> messages = Arrays.asList("Test message 1", "Other test message", "Third message for test");
        given(helpProvider.printHelp(sender, result, options)).willReturn(messages);

        // when
        commandService.outputHelp(sender, result, options);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender, times(3)).sendMessage(captor.capture());
        assertThat(captor.getAllValues(), equalTo(messages));
    }

    @Test
    public void shouldReturnManagementObject() {
        // given
        Management management = mock(Management.class);
        given(authMe.getManagement()).willReturn(management);

        // when
        Management result = commandService.getManagement();

        // then
        assertThat(result, equalTo(management));
        verify(authMe).getManagement();
    }

    @Test
    public void shouldReturnPermissionsManager() {
        // given / when
        PermissionsManager result = commandService.getPermissionsManager();

        // then
        assertThat(result, equalTo(permissionsManager));
    }

    @Test
    public void shouldRetrieveMessage() {
        // given
        MessageKey key = MessageKey.USAGE_CAPTCHA;
        String[] givenMessages = new String[]{"Lorem ipsum...", "Test line test"};
        given(messages.retrieve(key)).willReturn(givenMessages);

        // when
        String[] result = commandService.retrieveMessage(key);

        // then
        assertThat(result, equalTo(givenMessages));
        verify(messages).retrieve(key);
    }

    @Test
    public void shouldRetrieveProperty() {
        // given
        Property<Integer> property = SecuritySettings.CAPTCHA_LENGTH;
        given(settings.getProperty(property)).willReturn(7);

        // when
        int result = commandService.getProperty(property);

        // then
        assertThat(result, equalTo(7));
        verify(settings).getProperty(property);
    }

    @Test
    public void shouldReturnSettings() {
        // given/when
        NewSetting result = commandService.getSettings();

        // then
        assertThat(result, equalTo(settings));
    }

    @Test
    public void shouldReturnAuthMe() {
        // given/when
        AuthMe result = commandService.getAuthMe();

        // then
        assertThat(result, equalTo(authMe));
    }

    @Test
    public void shouldValidatePassword() {
        // given
        String user = "asdf";
        String password = "mySecret55";
        given(validationService.validatePassword(password, user)).willReturn(MessageKey.INVALID_PASSWORD_LENGTH);

        // when
        MessageKey result = commandService.validatePassword(password, user);

        // then
        assertThat(result, equalTo(MessageKey.INVALID_PASSWORD_LENGTH));
        verify(validationService).validatePassword(password, user);
    }

    @Test
    public void shouldValidateEmail() {
        // given
        String email = "test@example.tld";
        given(validationService.validateEmail(email)).willReturn(true);

        // when
        boolean result = commandService.validateEmail(email);

        // then
        assertThat(result, equalTo(true));
        verify(validationService).validateEmail(email);
    }

    @Test
    public void shouldCheckIfEmailCanBeUsed() {
        // given
        String email = "mail@example.com";
        CommandSender sender = mock(CommandSender.class);
        given(validationService.isEmailFreeForRegistration(email, sender))
            .willReturn(true);

        // when
        boolean result = commandService.isEmailFreeForRegistration(email, sender);

        // then
        assertThat(result, equalTo(true));
        verify(validationService).isEmailFreeForRegistration(email, sender);
    }

    @Test
    public void shouldGetPlayer() {
        // given
        String playerName = "_tester";
        Player player = mock(Player.class);
        given(bukkitService.getPlayerExact(playerName)).willReturn(player);

        // when
        Player result = commandService.getPlayer(playerName);

        // then
        assertThat(result, equalTo(player));
        verify(bukkitService).getPlayerExact(playerName);
    }

}
