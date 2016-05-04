package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import org.bukkit.command.CommandSender;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static fr.xephi.authme.TestHelper.runInnerRunnable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link ChangePasswordAdminCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ChangePasswordAdminCommandTest {

    @InjectMocks
    private ChangePasswordAdminCommand command;

    @Mock
    private CommandService service;

    @Mock
    private PasswordSecurity passwordSecurity;

    @Mock
    private DataSource dataSource;

    @Mock
    private PlayerCache playerCache;

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldRejectInvalidPassword() {
        // given
        CommandSender sender = mock(CommandSender.class);
        given(service.validatePassword("Bobby", "bobby")).willReturn(MessageKey.PASSWORD_IS_USERNAME_ERROR);

        // when
        command.executeCommand(sender, Arrays.asList("bobby", "Bobby"), service);

        // then
        verify(service).validatePassword("Bobby", "bobby");
        verify(service).send(sender, MessageKey.PASSWORD_IS_USERNAME_ERROR);
        verifyZeroInteractions(dataSource);
    }

    @Test
    public void shouldRejectCommandForUnknownUser() {
        // given
        CommandSender sender = mock(CommandSender.class);
        String player = "player";
        given(playerCache.isAuthenticated(player)).willReturn(false);
        given(dataSource.getAuth(player)).willReturn(null);

        // when
        command.executeCommand(sender, Arrays.asList(player, "password"), service);
        runInnerRunnable(service);

        // then
        verify(service).send(sender, MessageKey.UNKNOWN_USER);
        verify(dataSource, never()).updatePassword(any(PlayerAuth.class));
    }

    @Test
    public void shouldUpdatePasswordOfLoggedInUser() {
        // given
        CommandSender sender = mock(CommandSender.class);
        String player = "my_user12";
        String password = "passPass";
        PlayerAuth auth = mock(PlayerAuth.class);

        given(playerCache.isAuthenticated(player)).willReturn(true);
        given(playerCache.getAuth(player)).willReturn(auth);

        HashedPassword hashedPassword = mock(HashedPassword.class);
        given(passwordSecurity.computeHash(password, player)).willReturn(hashedPassword);
        given(dataSource.updatePassword(auth)).willReturn(true);

        // when
        command.executeCommand(sender, Arrays.asList(player, password), service);
        runInnerRunnable(service);

        // then
        verify(service).validatePassword(password, player);
        verify(service).send(sender, MessageKey.PASSWORD_CHANGED_SUCCESS);
        verify(passwordSecurity).computeHash(password, player);
        verify(auth).setPassword(hashedPassword);
        verify(dataSource).updatePassword(auth);
    }

    @Test
    public void shouldUpdatePasswordOfOfflineUser() {
        // given
        CommandSender sender = mock(CommandSender.class);
        String player = "my_user12";
        String password = "passPass";
        PlayerAuth auth = mock(PlayerAuth.class);
        given(playerCache.isAuthenticated(player)).willReturn(false);
        given(dataSource.isAuthAvailable(player)).willReturn(true);
        given(dataSource.getAuth(player)).willReturn(auth);
        given(dataSource.updatePassword(auth)).willReturn(true);

        HashedPassword hashedPassword = mock(HashedPassword.class);
        given(passwordSecurity.computeHash(password, player)).willReturn(hashedPassword);

        // when
        command.executeCommand(sender, Arrays.asList(player, password), service);
        runInnerRunnable(service);

        // then
        verify(service).validatePassword(password, player);
        verify(service).send(sender, MessageKey.PASSWORD_CHANGED_SUCCESS);
        verify(passwordSecurity).computeHash(password, player);
        verify(auth).setPassword(hashedPassword);
        verify(dataSource).updatePassword(auth);
    }

    @Test
    public void shouldReportWhenSaveFailed() {
        // given
        CommandSender sender = mock(CommandSender.class);
        String player = "my_user12";
        String password = "passPass";
        PlayerAuth auth = mock(PlayerAuth.class);
        given(playerCache.isAuthenticated(player)).willReturn(true);
        given(playerCache.getAuth(player)).willReturn(auth);

        HashedPassword hashedPassword = mock(HashedPassword.class);
        given(passwordSecurity.computeHash(password, player)).willReturn(hashedPassword);
        given(dataSource.updatePassword(auth)).willReturn(false);

        // when
        command.executeCommand(sender, Arrays.asList(player, password), service);
        runInnerRunnable(service);

        // then
        verify(service).validatePassword(password, player);
        verify(service).send(sender, MessageKey.ERROR);
        verify(passwordSecurity).computeHash(password, player);
        verify(auth).setPassword(hashedPassword);
        verify(dataSource).updatePassword(auth);
    }

}
