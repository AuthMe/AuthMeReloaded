package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static fr.xephi.authme.TestHelper.runOptionallyAsyncTask;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link SetEmailCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SetEmailCommandTest {

    @InjectMocks
    private SetEmailCommand command;

    @Mock
    private DataSource dataSource;

    @Mock
    private CommandService commandService;

    @Mock
    private PlayerCache playerCache;

    @Mock
    private BukkitService bukkitService;

    @Test
    public void shouldRejectInvalidMail() {
        // given
        String user = "somebody";
        String email = "some.test@example.org";
        given(commandService.validateEmail(email)).willReturn(false);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Arrays.asList(user, email));

        // then
        verify(commandService).validateEmail(email);
        verify(commandService).send(sender, MessageKey.INVALID_EMAIL);
        verifyZeroInteractions(dataSource);
    }

    @Test
    public void shouldHandleUnknownUser() {
        // given
        String user = "nonexistent";
        String email = "mail@example.com";
        given(commandService.validateEmail(email)).willReturn(true);
        given(dataSource.getAuth(user)).willReturn(null);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Arrays.asList(user, email));
        runOptionallyAsyncTask(bukkitService);

        // then
        verify(commandService).validateEmail(email);
        verify(dataSource).getAuth(user);
        verify(commandService).send(sender, MessageKey.UNKNOWN_USER);
        verifyNoMoreInteractions(dataSource);
    }

    @Test
    public void shouldHandleAlreadyTakenEmail() {
        // given
        String user = "someone";
        String email = "mail@example.com";
        given(commandService.validateEmail(email)).willReturn(true);
        PlayerAuth auth = mock(PlayerAuth.class);
        given(dataSource.getAuth(user)).willReturn(auth);
        CommandSender sender = mock(CommandSender.class);
        given(commandService.isEmailFreeForRegistration(email, sender)).willReturn(false);

        // when
        command.executeCommand(sender, Arrays.asList(user, email));
        runOptionallyAsyncTask(bukkitService);

        // then
        verify(commandService).validateEmail(email);
        verify(dataSource).getAuth(user);
        verify(commandService).isEmailFreeForRegistration(email, sender);
        verify(commandService).send(sender, MessageKey.EMAIL_ALREADY_USED_ERROR);
        verifyNoMoreInteractions(dataSource);
        verifyZeroInteractions(auth);
    }

    @Test
    public void shouldHandlePersistenceError() {
        // given
        String user = "Bobby";
        String email = "new-addr@example.org";
        given(commandService.validateEmail(email)).willReturn(true);
        PlayerAuth auth = mock(PlayerAuth.class);
        given(dataSource.getAuth(user)).willReturn(auth);
        CommandSender sender = mock(CommandSender.class);
        given(commandService.isEmailFreeForRegistration(email, sender)).willReturn(true);
        given(dataSource.updateEmail(auth)).willReturn(false);

        // when
        command.executeCommand(sender, Arrays.asList(user, email));
        runOptionallyAsyncTask(bukkitService);

        // then
        verify(commandService).validateEmail(email);
        verify(dataSource).getAuth(user);
        verify(commandService).isEmailFreeForRegistration(email, sender);
        verify(commandService).send(sender, MessageKey.ERROR);
        verify(dataSource).updateEmail(auth);
        verifyNoMoreInteractions(dataSource);
    }

    @Test
    public void shouldUpdateEmail() {
        // given
        String user = "Bobby";
        String email = "new-addr@example.org";
        given(commandService.validateEmail(email)).willReturn(true);
        PlayerAuth auth = mock(PlayerAuth.class);
        given(dataSource.getAuth(user)).willReturn(auth);
        CommandSender sender = mock(CommandSender.class);
        given(commandService.isEmailFreeForRegistration(email, sender)).willReturn(true);
        given(dataSource.updateEmail(auth)).willReturn(true);
        given(playerCache.getAuth(user)).willReturn(null);

        // when
        command.executeCommand(sender, Arrays.asList(user, email));
        runOptionallyAsyncTask(bukkitService);

        // then
        verify(commandService).validateEmail(email);
        verify(dataSource).getAuth(user);
        verify(commandService).isEmailFreeForRegistration(email, sender);
        verify(commandService).send(sender, MessageKey.EMAIL_CHANGED_SUCCESS);
        verify(dataSource).updateEmail(auth);
        verify(playerCache, never()).updatePlayer(any(PlayerAuth.class));
        verifyNoMoreInteractions(dataSource);
    }

    @Test
    public void shouldUpdateEmailAndPlayerCache() {
        // given
        String user = "Bobby";
        String email = "new-addr@example.org";
        given(commandService.validateEmail(email)).willReturn(true);
        PlayerAuth auth = mock(PlayerAuth.class);
        given(dataSource.getAuth(user)).willReturn(auth);
        CommandSender sender = mock(CommandSender.class);
        given(commandService.isEmailFreeForRegistration(email, sender)).willReturn(true);
        given(dataSource.updateEmail(auth)).willReturn(true);
        given(playerCache.getAuth(user)).willReturn(mock(PlayerAuth.class));

        // when
        command.executeCommand(sender, Arrays.asList(user, email));
        runOptionallyAsyncTask(bukkitService);

        // then
        verify(commandService).validateEmail(email);
        verify(dataSource).getAuth(user);
        verify(commandService).isEmailFreeForRegistration(email, sender);
        verify(commandService).send(sender, MessageKey.EMAIL_CHANGED_SUCCESS);
        verify(dataSource).updateEmail(auth);
        verify(playerCache).updatePlayer(auth);
        verifyNoMoreInteractions(dataSource);
    }

}
