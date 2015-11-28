package fr.xephi.authme.command.executable.changepassword;

import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.settings.MessageKey;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.WrapperMock;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Test for {@link ChangePasswordCommand}.
 */
public class ChangePasswordCommandTest {

    private Messages messagesMock;
    private PlayerCache cacheMock;

    @Before
    public void setUpMocks() {
        WrapperMock wrapper = WrapperMock.createInstance();
        messagesMock = wrapper.getMessages();
        cacheMock = wrapper.getPlayerCache();

        // Only allow passwords with alphanumerical characters for the test
        Settings.getPassRegex = "[a-zA-Z0-9]+";
        Settings.getPasswordMinLen = 2;
        Settings.passwordMaxLength = 50;
        // TODO ljacqu 20151126: Verify the calls to getServer() (see commented code)
    }

    @Test
    public void shouldRejectNonPlayerSender() {
        // given
        CommandSender sender = mock(BlockCommandSender.class);
        ChangePasswordCommand command = new ChangePasswordCommand();
        CommandParts arguments = mock(CommandParts.class);

        // when
        command.executeCommand(sender, new CommandParts(), arguments);

        // then
        verify(arguments, never()).get(anyInt());
        //verify(pluginMock, never()).getServer();
    }

    @Test
    public void shouldRejectNotLoggedInPlayer() {
        // given
        CommandSender sender = initPlayerWithName("name", false);
        ChangePasswordCommand command = new ChangePasswordCommand();

        // when
        command.executeCommand(sender, new CommandParts(), new CommandParts("pass"));

        // then
        verify(messagesMock).send(sender, MessageKey.NOT_LOGGED_IN);
        //verify(pluginMock, never()).getServer();
    }

    @Test
    public void shouldDenyInvalidPassword() {
        // given
        CommandSender sender = initPlayerWithName("name", true);
        ChangePasswordCommand command = new ChangePasswordCommand();

        // when
        command.executeCommand(sender, new CommandParts(), new CommandParts("!pass"));

        // then
        verify(messagesMock).send(sender, MessageKey.PASSWORD_MATCH_ERROR);
        //verify(pluginMock, never()).getServer();
    }


    @Test
    public void shouldRejectPasswordEqualToNick() {
        // given
        CommandSender sender = initPlayerWithName("tester", true);
        ChangePasswordCommand command = new ChangePasswordCommand();

        // when
        command.executeCommand(sender, new CommandParts(), new CommandParts("Tester"));

        // then
        verify(messagesMock).send(sender, MessageKey.PASSWORD_IS_USERNAME_ERROR);
        //verify(pluginMock, never()).getServer();
    }

    @Test
    public void shouldRejectTooLongPassword() {
        // given
        CommandSender sender = initPlayerWithName("abc12", true);
        ChangePasswordCommand command = new ChangePasswordCommand();
        Settings.passwordMaxLength = 3;

        // when
        command.executeCommand(sender, new CommandParts(), new CommandParts("test"));

        // then
        verify(messagesMock).send(sender, MessageKey.INVALID_PASSWORD_LENGTH);
        //verify(pluginMock, never()).getServer();
    }

    @Test
    public void shouldRejectTooShortPassword() {
        // given
        CommandSender sender = initPlayerWithName("abc12", true);
        ChangePasswordCommand command = new ChangePasswordCommand();
        Settings.getPasswordMinLen = 7;

        // when
        command.executeCommand(sender, new CommandParts(), new CommandParts("tester"));

        // then
        verify(messagesMock).send(sender, MessageKey.INVALID_PASSWORD_LENGTH);
        //verify(pluginMock, never()).getServer();
    }

    @Test
    public void shouldRejectUnsafeCustomPassword() {
        // given
        CommandSender sender = initPlayerWithName("player", true);
        ChangePasswordCommand command = new ChangePasswordCommand();
        Settings.unsafePasswords = Arrays.asList("test", "abc123");

        // when
        command.executeCommand(sender, new CommandParts(), new CommandParts("abc123"));

        // then
        verify(messagesMock).send(sender, MessageKey.PASSWORD_UNSAFE_ERROR);
        //verify(pluginMock, never()).getServer();
    }

    private Player initPlayerWithName(String name, boolean loggedIn) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(cacheMock.isAuthenticated(name)).thenReturn(loggedIn);
        return player;
    }

}
