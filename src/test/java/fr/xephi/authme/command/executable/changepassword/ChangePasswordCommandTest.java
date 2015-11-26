package fr.xephi.authme.command.executable.changepassword;

import fr.xephi.authme.AuthMeMockUtil;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.junit.Before;
import org.junit.Test;


import java.util.Arrays;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ChangePasswordCommand}.
 */
public class ChangePasswordCommandTest {

    private Messages messagesMock;
    private PlayerCache cacheMock;

    @Before
    public void setUpMocks() {
        AuthMeMockUtil.mockAuthMeInstance();
        AuthMeMockUtil.mockPlayerCacheInstance();
        cacheMock = PlayerCache.getInstance();

        AuthMeMockUtil.mockMessagesInstance();
        messagesMock = Messages.getInstance();

        // Only allow passwords with alphanumerical characters for the test
        Settings.getPassRegex = "[a-zA-Z0-9]+";
        Settings.getPasswordMinLen = 2;
        Settings.passwordMaxLength = 50;

        // TODO ljacqu 20151121: Cannot mock getServer() as it's final
        // Probably the Command class should delegate as the others do
        /*
        Server server = Mockito.mock(Server.class);
        schedulerMock = Mockito.mock(BukkitScheduler.class);
        Mockito.when(server.getScheduler()).thenReturn(schedulerMock);
        Mockito.when(pluginMock.getServer()).thenReturn(server);
        */
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
        verify(messagesMock).send(sender, "not_logged_in");
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
        verify(messagesMock).send(sender, "password_error");
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
        verify(messagesMock).send(sender, "password_error_nick");
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
        verify(messagesMock).send(sender, "pass_len");
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
        verify(messagesMock).send(sender, "pass_len");
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
        verify(messagesMock).send(sender, "password_error_unsafe");
        //verify(pluginMock, never()).getServer();
    }

    private Player initPlayerWithName(String name, boolean loggedIn) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(cacheMock.isAuthenticated(name)).thenReturn(loggedIn);
        return player;
    }

}
