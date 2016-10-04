package fr.xephi.authme.command.executable.changepassword;

import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.service.ValidationService.ValidationResult;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ChangePasswordCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ChangePasswordCommandTest {

    @InjectMocks
    private ChangePasswordCommand command;

    @Mock
    private CommandService commandService;

    @Mock
    private PlayerCache playerCache;

    @Mock
    private ValidationService validationService;

    @Mock
    private Management management;

    @Before
    public void setSettings() {
        when(commandService.getProperty(SecuritySettings.MIN_PASSWORD_LENGTH)).thenReturn(2);
        when(commandService.getProperty(SecuritySettings.MAX_PASSWORD_LENGTH)).thenReturn(50);
        // Only allow passwords with alphanumerical characters for the test
        when(commandService.getProperty(RestrictionSettings.ALLOWED_PASSWORD_REGEX)).thenReturn("[a-zA-Z0-9]+");
        when(commandService.getProperty(SecuritySettings.UNSAFE_PASSWORDS)).thenReturn(Collections.<String>emptyList());
    }

    @Test
    public void shouldRejectNonPlayerSender() {
        // given
        CommandSender sender = mock(BlockCommandSender.class);

        // when
        command.executeCommand(sender, new ArrayList<String>());

        // then
        verify(sender).sendMessage(argThat(containsString("only for players")));
    }

    @Test
    public void shouldRejectNotLoggedInPlayer() {
        // given
        CommandSender sender = initPlayerWithName("name", false);

        // when
        command.executeCommand(sender, Arrays.asList("pass", "pass"));

        // then
        verify(commandService).send(sender, MessageKey.NOT_LOGGED_IN);
    }

    @Test
    public void shouldRejectInvalidPassword() {
        // given
        CommandSender sender = initPlayerWithName("abc12", true);
        String password = "newPW";
        given(validationService.validatePassword(password, "abc12"))
            .willReturn(new ValidationResult(MessageKey.INVALID_PASSWORD_LENGTH));

        // when
        command.executeCommand(sender, Arrays.asList("tester", password));

        // then
        verify(validationService).validatePassword(password, "abc12");
        verify(commandService).send(sender, MessageKey.INVALID_PASSWORD_LENGTH, new String[0]);
    }

    @Test
    public void shouldForwardTheDataForValidPassword() {
        // given
        String oldPass = "oldpass";
        String newPass = "abc123";
        Player player = initPlayerWithName("parker", true);
        given(validationService.validatePassword("abc123", "parker")).willReturn(new ValidationResult());

        // when
        command.executeCommand(player, Arrays.asList(oldPass, newPass));

        // then
        verify(validationService).validatePassword(newPass, "parker");
        verify(commandService, never()).send(eq(player), any(MessageKey.class));
        verify(management).performPasswordChange(player, oldPass, newPass);
    }

    private Player initPlayerWithName(String name, boolean loggedIn) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(playerCache.isAuthenticated(name)).thenReturn(loggedIn);
        return player;
    }

}
