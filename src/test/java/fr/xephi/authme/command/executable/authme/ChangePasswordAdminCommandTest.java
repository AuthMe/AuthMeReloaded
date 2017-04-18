package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.service.ValidationService.ValidationResult;
import org.bukkit.command.CommandSender;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;

import static fr.xephi.authme.TestHelper.runOptionallyAsyncTask;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
    private CommonService service;

    @Mock
    private PasswordSecurity passwordSecurity;

    @Mock
    private DataSource dataSource;

    @Mock
    private PlayerCache playerCache;

    @Mock
    private BukkitService bukkitService;

    @Mock
    private ValidationService validationService;

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldRejectInvalidPassword() {
        // given
        CommandSender sender = mock(CommandSender.class);
        given(validationService.validatePassword("Bobby", "bobby")).willReturn(
            new ValidationResult(MessageKey.PASSWORD_IS_USERNAME_ERROR));

        // when
        command.executeCommand(sender, Arrays.asList("bobby", "Bobby"));

        // then
        verify(validationService).validatePassword("Bobby", "bobby");
        verify(service).send(sender, MessageKey.PASSWORD_IS_USERNAME_ERROR, new String[0]);
        verifyZeroInteractions(dataSource);
    }

    @Test
    public void shouldRejectCommandForUnknownUser() {
        // given
        CommandSender sender = mock(CommandSender.class);
        String player = "player";
        String password = "password";
        given(playerCache.isAuthenticated(player)).willReturn(false);
        given(validationService.validatePassword(password, player)).willReturn(new ValidationResult());

        // when
        command.executeCommand(sender, Arrays.asList(player, password));
        runOptionallyAsyncTask(bukkitService);

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
        given(playerCache.isAuthenticated(player)).willReturn(true);

        HashedPassword hashedPassword = mock(HashedPassword.class);
        given(passwordSecurity.computeHash(password, player)).willReturn(hashedPassword);
        given(dataSource.updatePassword(player, hashedPassword)).willReturn(true);
        given(validationService.validatePassword(password, player)).willReturn(new ValidationResult());

        // when
        command.executeCommand(sender, Arrays.asList(player, password));
        runOptionallyAsyncTask(bukkitService);

        // then
        verify(validationService).validatePassword(password, player);
        verify(service).send(sender, MessageKey.PASSWORD_CHANGED_SUCCESS);
        verify(passwordSecurity).computeHash(password, player);
        verify(dataSource).updatePassword(player, hashedPassword);
    }

    @Test
    public void shouldUpdatePasswordOfOfflineUser() {
        // given
        CommandSender sender = mock(CommandSender.class);
        String player = "my_user12";
        String password = "passPass";
        given(playerCache.isAuthenticated(player)).willReturn(false);
        given(dataSource.isAuthAvailable(player)).willReturn(true);
        given(validationService.validatePassword(password, player)).willReturn(new ValidationResult());

        HashedPassword hashedPassword = mock(HashedPassword.class);
        given(passwordSecurity.computeHash(password, player)).willReturn(hashedPassword);
        given(dataSource.updatePassword(player, hashedPassword)).willReturn(true);

        // when
        command.executeCommand(sender, Arrays.asList(player, password));
        runOptionallyAsyncTask(bukkitService);

        // then
        verify(validationService).validatePassword(password, player);
        verify(service).send(sender, MessageKey.PASSWORD_CHANGED_SUCCESS);
        verify(passwordSecurity).computeHash(password, player);
        verify(dataSource).updatePassword(player, hashedPassword);
    }

    @Test
    public void shouldReportWhenSaveFailed() {
        // given
        CommandSender sender = mock(CommandSender.class);
        String player = "my_user12";
        String password = "passPass";
        given(playerCache.isAuthenticated(player)).willReturn(true);
        given(validationService.validatePassword(password, player)).willReturn(new ValidationResult());

        HashedPassword hashedPassword = mock(HashedPassword.class);
        given(passwordSecurity.computeHash(password, player)).willReturn(hashedPassword);
        given(dataSource.updatePassword(player, hashedPassword)).willReturn(false);

        // when
        command.executeCommand(sender, Arrays.asList(player, password));
        runOptionallyAsyncTask(bukkitService);

        // then
        verify(validationService).validatePassword(password, player);
        verify(service).send(sender, MessageKey.ERROR);
        verify(passwordSecurity).computeHash(password, player);
        verify(dataSource).updatePassword(player, hashedPassword);
    }

}
