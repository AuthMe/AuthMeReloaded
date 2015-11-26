package fr.xephi.authme.command.executable.register;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.AuthMeMockUtil;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.settings.MessageKey;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link RegisterCommand}.
 */
public class RegisterCommandTest {

    private static Management managementMock;
    private static Messages messagesMock;

    @Before
    public void initializeAuthMeMock() {
        AuthMeMockUtil.mockAuthMeInstance();
        AuthMe pluginMock = AuthMe.getInstance();

        messagesMock = mock(Messages.class);
        Mockito.when(pluginMock.getMessages()).thenReturn(messagesMock);

        Settings.captchaLength = 10;
        managementMock = mock(Management.class);
        Mockito.when(pluginMock.getManagement()).thenReturn(managementMock);
    }

    @Test
    public void shouldNotRunForNonPlayerSender() {
        // given
        CommandSender sender = mock(BlockCommandSender.class);
        RegisterCommand command = new RegisterCommand();
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        // when
        command.executeCommand(sender, new CommandParts(), new CommandParts());

        // then
        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().contains("Player Only!"), equalTo(true));
        verify(managementMock, never()).performRegister(any(Player.class), anyString(), anyString());
    }

    @Test
    public void shouldFailForEmptyArguments() {
        // given
        CommandSender sender = mock(Player.class);
        RegisterCommand command = new RegisterCommand();

        // when
        command.executeCommand(sender, new CommandParts(), new CommandParts());

        // then
        verify(messagesMock).send(sender, MessageKey.USAGE_REGISTER);
        verify(managementMock, never()).performRegister(any(Player.class), anyString(), anyString());
    }

    @Test
    public void shouldForwardRegister() {
        // given
        Player sender = mock(Player.class);
        RegisterCommand command = new RegisterCommand();

        // when
        command.executeCommand(sender, new CommandParts(), new CommandParts("password"));

        // then
        verify(managementMock).performRegister(sender, "password", "");
    }
}
