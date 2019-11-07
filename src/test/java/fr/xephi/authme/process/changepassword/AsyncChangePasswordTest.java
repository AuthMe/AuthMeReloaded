package fr.xephi.authme.process.changepassword;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.bungeecord.BungeeSender;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link AsyncChangePassword}.
 */
@ExtendWith(MockitoExtension.class)
class AsyncChangePasswordTest {

    @InjectMocks
    private AsyncChangePassword asyncChangePassword;

    @Mock
    private CommonService commonService;
    @Mock
    private DataSource dataSource;
    @Mock
    private PlayerCache playerCache;
    @Mock
    private PasswordSecurity passwordSecurity;
    @Mock
    private BungeeSender bungeeSender;

    @BeforeEach
    void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Test
    void shouldRejectCommandForUnknownUser() {
        // given
        CommandSender sender = mock(CommandSender.class);
        String player = "player";
        String password = "password";
        given(playerCache.isAuthenticated(player)).willReturn(false);
        given(dataSource.isAuthAvailable(player)).willReturn(false);

        // when
        asyncChangePassword.changePasswordAsAdmin(sender, player, password);

        // then
        verify(commonService).send(sender, MessageKey.UNKNOWN_USER);
        verify(dataSource, only()).isAuthAvailable(player);
    }

    @Test
    void shouldUpdatePasswordOfLoggedInUser() {
        // given
        CommandSender sender = mock(CommandSender.class);
        String player = "my_user12";
        String password = "passPass";
        given(playerCache.isAuthenticated(player)).willReturn(true);

        HashedPassword hashedPassword = mock(HashedPassword.class);
        given(passwordSecurity.computeHash(password, player)).willReturn(hashedPassword);
        given(dataSource.updatePassword(player, hashedPassword)).willReturn(true);

        // when
        asyncChangePassword.changePasswordAsAdmin(sender, player, password);

        // then
        verify(commonService).send(sender, MessageKey.PASSWORD_CHANGED_SUCCESS);
        verify(passwordSecurity).computeHash(password, player);
        verify(dataSource).updatePassword(player, hashedPassword);
    }

    @Test
    void shouldUpdatePasswordOfOfflineUser() {
        // given
        CommandSender sender = mock(CommandSender.class);
        String player = "my_user12";
        String password = "passPass";
        given(playerCache.isAuthenticated(player)).willReturn(false);
        given(dataSource.isAuthAvailable(player)).willReturn(true);

        HashedPassword hashedPassword = mock(HashedPassword.class);
        given(passwordSecurity.computeHash(password, player)).willReturn(hashedPassword);
        given(dataSource.updatePassword(player, hashedPassword)).willReturn(true);

        // when
        asyncChangePassword.changePasswordAsAdmin(sender, player, password);

        // then
        verify(commonService).send(sender, MessageKey.PASSWORD_CHANGED_SUCCESS);
        verify(passwordSecurity).computeHash(password, player);
        verify(dataSource).updatePassword(player, hashedPassword);
    }

    @Test
    void shouldReportWhenSaveFailed() {
        // given
        CommandSender sender = mock(CommandSender.class);
        String player = "my_user12";
        String password = "passPass";
        given(playerCache.isAuthenticated(player)).willReturn(true);

        HashedPassword hashedPassword = mock(HashedPassword.class);
        given(passwordSecurity.computeHash(password, player)).willReturn(hashedPassword);
        given(dataSource.updatePassword(player, hashedPassword)).willReturn(false);

        // when
        asyncChangePassword.changePasswordAsAdmin(sender, player, password);

        // then
        verify(commonService).send(sender, MessageKey.ERROR);
        verify(passwordSecurity).computeHash(password, player);
        verify(dataSource).updatePassword(player, hashedPassword);
    }

}
