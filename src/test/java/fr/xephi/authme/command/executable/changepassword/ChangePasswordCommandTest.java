package fr.xephi.authme.command.executable.changepassword;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.settings.MessageKey;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.ChangePasswordTask;
import fr.xephi.authme.util.WrapperMock;
import org.bukkit.Server;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Test for {@link ChangePasswordCommand}.
 */
public class ChangePasswordCommandTest {

    private WrapperMock wrapperMock;
    private Messages messagesMock;
    private PlayerCache cacheMock;

    @Before
    public void setUpMocks() {
        wrapperMock = WrapperMock.createInstance();
        messagesMock = wrapperMock.getMessages();
        cacheMock = wrapperMock.getPlayerCache();

        // Only allow passwords with alphanumerical characters for the test
        Settings.getPassRegex = "[a-zA-Z0-9]+";
        Settings.getPasswordMinLen = 2;
        Settings.passwordMaxLength = 50;
        Settings.unsafePasswords = Collections.EMPTY_LIST;
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
        assertThat(wrapperMock.wasMockCalled(Server.class), equalTo(false));
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
        assertThat(wrapperMock.wasMockCalled(Server.class), equalTo(false));
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
        assertThat(wrapperMock.wasMockCalled(Server.class), equalTo(false));
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
        assertThat(wrapperMock.wasMockCalled(Server.class), equalTo(false));
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
        assertThat(wrapperMock.wasMockCalled(Server.class), equalTo(false));
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
        assertThat(wrapperMock.wasMockCalled(Server.class), equalTo(false));
    }

    @Test
    public void shouldRejectUnsafeCustomPassword() {
        // given
        CommandSender sender = initPlayerWithName("player", true);
        ChangePasswordCommand command = new ChangePasswordCommand();
        Settings.unsafePasswords = asList("test", "abc123");

        // when
        command.executeCommand(sender, new CommandParts(), new CommandParts("abc123"));

        // then
        verify(messagesMock).send(sender, MessageKey.PASSWORD_UNSAFE_ERROR);
        assertThat(wrapperMock.wasMockCalled(Server.class), equalTo(false));
    }

    @Test
    public void shouldForwardTheDataForValidPassword() {
        // given
        CommandSender sender = initPlayerWithName("parker", true);
        ChangePasswordCommand command = new ChangePasswordCommand();
        BukkitScheduler schedulerMock = mock(BukkitScheduler.class);
        given(wrapperMock.getServer().getScheduler()).willReturn(schedulerMock);

        // when
        command.executeCommand(sender, new CommandParts(), new CommandParts(asList("abc123", "abc123")));

        // then
        verify(messagesMock, never()).send(eq(sender), any(MessageKey.class));
        ArgumentCaptor<ChangePasswordTask> taskCaptor = ArgumentCaptor.forClass(ChangePasswordTask.class);
        verify(schedulerMock).runTaskAsynchronously(any(AuthMe.class), taskCaptor.capture());
        ChangePasswordTask task = taskCaptor.getValue();
        assertThat((String) ReflectionTestUtils.getFieldValue(ChangePasswordTask.class, task, "newPassword"),
            equalTo("abc123"));
    }

    private Player initPlayerWithName(String name, boolean loggedIn) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(cacheMock.isAuthenticated(name)).thenReturn(loggedIn);
        return player;
    }

}
