package fr.xephi.authme.command;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.help.HelpProvider;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.custom.NewSetting;
import fr.xephi.authme.settings.custom.SecuritySettings;
import fr.xephi.authme.settings.domain.Property;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link CommandService}.
 */
public class CommandServiceTest {

    private AuthMe authMe;
    private CommandMapper commandMapper;
    private HelpProvider helpProvider;
    private Messages messages;
    private PasswordSecurity passwordSecurity;
    private CommandService commandService;
    private PermissionsManager permissionsManager;
    private NewSetting settings;

    @Before
    public void setUpService() {
        authMe = mock(AuthMe.class);
        commandMapper = mock(CommandMapper.class);
        helpProvider = mock(HelpProvider.class);
        messages = mock(Messages.class);
        passwordSecurity = mock(PasswordSecurity.class);
        permissionsManager = mock(PermissionsManager.class);
        settings = mock(NewSetting.class);
        commandService = new CommandService(
            authMe, commandMapper, helpProvider, messages, passwordSecurity, permissionsManager, settings);
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
    @Ignore
    public void shouldRunTaskInAsync() {
        // given
        Runnable runnable = mock(Runnable.class);

        // when
        commandService.runTaskAsynchronously(runnable);

        // then
        // TODO ljacqu 20151226: AuthMe#getServer() is final, i.e. not mockable
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
        int result = settings.getProperty(property);

        // then
        assertThat(result, equalTo(7));
        verify(settings).getProperty(property);
    }
}
