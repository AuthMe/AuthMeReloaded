package fr.xephi.authme.command.executable.changepassword;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.task.ChangePasswordTask;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ChangePasswordCommand}.
 */
public class ChangePasswordCommandTest {

    private CommandService commandService;

    @Before
    public void setUpMocks() {
        commandService = mock(CommandService.class);

        when(commandService.getProperty(SecuritySettings.MIN_PASSWORD_LENGTH)).thenReturn(2);
        when(commandService.getProperty(SecuritySettings.MAX_PASSWORD_LENGTH)).thenReturn(50);
        // Only allow passwords with alphanumerical characters for the test
        when(commandService.getProperty(RestrictionSettings.ALLOWED_PASSWORD_REGEX)).thenReturn("[a-zA-Z0-9]+");
        when(commandService.getProperty(SecuritySettings.UNSAFE_PASSWORDS)).thenReturn(Collections.EMPTY_LIST);
    }

    @Test
    public void shouldRejectNonPlayerSender() {
        // given
        CommandSender sender = mock(BlockCommandSender.class);
        ChangePasswordCommand command = new ChangePasswordCommand();

        // when
        command.executeCommand(sender, new ArrayList<String>(), commandService);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender).sendMessage(captor.capture());
        assertThat(captor.getValue(), containsString("only for players"));
    }

    @Test
    public void shouldRejectNotLoggedInPlayer() {
        // given
        CommandSender sender = initPlayerWithName("name", false);
        ChangePasswordCommand command = new ChangePasswordCommand();

        // when
        command.executeCommand(sender, Arrays.asList("pass", "pass"), commandService);

        // then
        verify(commandService).send(sender, MessageKey.NOT_LOGGED_IN);
    }

    @Test
    public void shouldDenyInvalidPassword() {
        // given
        CommandSender sender = initPlayerWithName("name", true);
        ChangePasswordCommand command = new ChangePasswordCommand();

        // when
        command.executeCommand(sender, Arrays.asList("old123", "!pass"), commandService);

        // then
        verify(commandService).send(sender, MessageKey.PASSWORD_MATCH_ERROR);
    }


    @Test
    public void shouldRejectPasswordEqualToNick() {
        // given
        CommandSender sender = initPlayerWithName("tester", true);
        ChangePasswordCommand command = new ChangePasswordCommand();

        // when
        command.executeCommand(sender, Arrays.asList("old_", "Tester"), commandService);

        // then
        verify(commandService).send(sender, MessageKey.PASSWORD_IS_USERNAME_ERROR);
    }

    @Test
    public void shouldRejectTooLongPassword() {
        // given
        CommandSender sender = initPlayerWithName("abc12", true);
        ChangePasswordCommand command = new ChangePasswordCommand();
        given(commandService.getProperty(SecuritySettings.MAX_PASSWORD_LENGTH)).willReturn(3);

        // when
        command.executeCommand(sender, Arrays.asList("12", "test"), commandService);

        // then
        verify(commandService).send(sender, MessageKey.INVALID_PASSWORD_LENGTH);
    }

    @Test
    public void shouldRejectTooShortPassword() {
        // given
        CommandSender sender = initPlayerWithName("abc12", true);
        ChangePasswordCommand command = new ChangePasswordCommand();
        given(commandService.getProperty(SecuritySettings.MIN_PASSWORD_LENGTH)).willReturn(7);

        // when
        command.executeCommand(sender, Arrays.asList("oldverylongpassword", "tester"), commandService);

        // then
        verify(commandService).send(sender, MessageKey.INVALID_PASSWORD_LENGTH);
    }

    @Test
    public void shouldRejectUnsafeCustomPassword() {
        // given
        CommandSender sender = initPlayerWithName("player", true);
        ChangePasswordCommand command = new ChangePasswordCommand();
        given(commandService.getProperty(SecuritySettings.UNSAFE_PASSWORDS))
            .willReturn(Arrays.asList("test", "abc123"));

        // when
        command.executeCommand(sender, Arrays.asList("oldpw", "abc123"), commandService);

        // then
        verify(commandService).send(sender, MessageKey.PASSWORD_UNSAFE_ERROR);
    }

    @Test
    public void shouldForwardTheDataForValidPassword() {
        // given
        CommandSender sender = initPlayerWithName("parker", true);
        ChangePasswordCommand command = new ChangePasswordCommand();

        // when
        command.executeCommand(sender, Arrays.asList("abc123", "abc123"), commandService);

        // then
        verify(commandService, never()).send(eq(sender), any(MessageKey.class));
        ArgumentCaptor<ChangePasswordTask> taskCaptor = ArgumentCaptor.forClass(ChangePasswordTask.class);
        verify(commandService).runTaskAsynchronously(taskCaptor.capture());
        ChangePasswordTask task = taskCaptor.getValue();
        assertThat((String) ReflectionTestUtils.getFieldValue(ChangePasswordTask.class, task, "newPassword"),
            equalTo("abc123"));
    }

    private Player initPlayerWithName(String name, boolean loggedIn) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        PlayerCache playerCache = mock(PlayerCache.class);
        when(playerCache.isAuthenticated(name)).thenReturn(loggedIn);
        when(commandService.getPlayerCache()).thenReturn(playerCache);
        return player;
    }

}
