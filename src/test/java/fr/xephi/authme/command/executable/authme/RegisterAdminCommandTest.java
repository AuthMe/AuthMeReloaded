package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link RegisterAdminCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class RegisterAdminCommandTest {

    @Mock
    private CommandSender sender;
    @Mock
    private CommandService commandService;

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldRejectInvalidPassword() {
        // given
        String user = "tester";
        String password = "myPassword";
        given(commandService.validatePassword(password, user)).willReturn(MessageKey.INVALID_PASSWORD_LENGTH);
        ExecutableCommand command = new RegisterAdminCommand();

        // when
        command.executeCommand(sender, Arrays.asList(user, password), commandService);

        // then
        verify(commandService).validatePassword(password, user);
        verify(commandService).send(sender, MessageKey.INVALID_PASSWORD_LENGTH);
        verify(commandService, never()).runTaskAsynchronously(any(Runnable.class));
    }

    @Test
    public void shouldRejectAlreadyRegisteredAccount() {
        // given
        String user = "my_name55";
        String password = "@some-pass@";
        given(commandService.validatePassword(password, user)).willReturn(null);
        DataSource dataSource = mock(DataSource.class);
        given(dataSource.isAuthAvailable(user)).willReturn(true);
        given(commandService.getDataSource()).willReturn(dataSource);
        ExecutableCommand command = new RegisterAdminCommand();

        // when
        command.executeCommand(sender, Arrays.asList(user, password), commandService);
        TestHelper.runInnerRunnable(commandService);

        // then
        verify(commandService).validatePassword(password, user);
        verify(commandService).send(sender, MessageKey.NAME_ALREADY_REGISTERED);
        verify(dataSource, never()).saveAuth(any(PlayerAuth.class));
    }

    @Test
    public void shouldHandleSavingError() {
        // given
        String user = "test-test";
        String password = "afdjhfkt";
        given(commandService.validatePassword(password, user)).willReturn(null);
        DataSource dataSource = mock(DataSource.class);
        given(dataSource.isAuthAvailable(user)).willReturn(false);
        given(dataSource.saveAuth(any(PlayerAuth.class))).willReturn(false);
        given(commandService.getDataSource()).willReturn(dataSource);
        PasswordSecurity passwordSecurity = mock(PasswordSecurity.class);
        HashedPassword hashedPassword = new HashedPassword("235sdf4w5udsgf");
        given(passwordSecurity.computeHash(password, user)).willReturn(hashedPassword);
        given(commandService.getPasswordSecurity()).willReturn(passwordSecurity);
        ExecutableCommand command = new RegisterAdminCommand();

        // when
        command.executeCommand(sender, Arrays.asList(user, password), commandService);
        TestHelper.runInnerRunnable(commandService);

        // then
        verify(commandService).validatePassword(password, user);
        verify(commandService).send(sender, MessageKey.ERROR);
        ArgumentCaptor<PlayerAuth> captor = ArgumentCaptor.forClass(PlayerAuth.class);
        verify(dataSource).saveAuth(captor.capture());
        assertAuthHasInfo(captor.getValue(), user, hashedPassword);
    }

    @Test
    public void shouldRegisterOfflinePlayer() {
        // given
        String user = "someone";
        String password = "Al1O3P49S5%";
        given(commandService.validatePassword(password, user)).willReturn(null);
        DataSource dataSource = mock(DataSource.class);
        given(dataSource.isAuthAvailable(user)).willReturn(false);
        given(dataSource.saveAuth(any(PlayerAuth.class))).willReturn(true);
        given(commandService.getDataSource()).willReturn(dataSource);
        PasswordSecurity passwordSecurity = mock(PasswordSecurity.class);
        HashedPassword hashedPassword = new HashedPassword("$aea2345EW235dfsa@#R%987048");
        given(passwordSecurity.computeHash(password, user)).willReturn(hashedPassword);
        given(commandService.getPasswordSecurity()).willReturn(passwordSecurity);
        given(commandService.getPlayer(user)).willReturn(null);
        ExecutableCommand command = new RegisterAdminCommand();

        // when
        command.executeCommand(sender, Arrays.asList(user, password), commandService);
        TestHelper.runInnerRunnable(commandService);

        // then
        verify(commandService).validatePassword(password, user);
        verify(commandService).send(sender, MessageKey.REGISTER_SUCCESS);
        ArgumentCaptor<PlayerAuth> captor = ArgumentCaptor.forClass(PlayerAuth.class);
        verify(dataSource).saveAuth(captor.capture());
        assertAuthHasInfo(captor.getValue(), user, hashedPassword);
        verify(dataSource).setUnlogged(user);
    }

    @Test
    public void shouldRegisterOnlinePlayer() {
        // given
        String user = "someone";
        String password = "Al1O3P49S5%";
        given(commandService.validatePassword(password, user)).willReturn(null);
        DataSource dataSource = mock(DataSource.class);
        given(dataSource.isAuthAvailable(user)).willReturn(false);
        given(dataSource.saveAuth(any(PlayerAuth.class))).willReturn(true);
        given(commandService.getDataSource()).willReturn(dataSource);
        PasswordSecurity passwordSecurity = mock(PasswordSecurity.class);
        HashedPassword hashedPassword = new HashedPassword("$aea2345EW235dfsa@#R%987048");
        given(passwordSecurity.computeHash(password, user)).willReturn(hashedPassword);
        given(commandService.getPasswordSecurity()).willReturn(passwordSecurity);
        Player player = mock(Player.class);
        given(commandService.getPlayer(user)).willReturn(player);
        ExecutableCommand command = new RegisterAdminCommand();

        // when
        command.executeCommand(sender, Arrays.asList(user, password), commandService);
        TestHelper.runInnerRunnable(commandService);

        // then
        verify(commandService).validatePassword(password, user);
        verify(commandService).send(sender, MessageKey.REGISTER_SUCCESS);
        ArgumentCaptor<PlayerAuth> captor = ArgumentCaptor.forClass(PlayerAuth.class);
        verify(dataSource).saveAuth(captor.capture());
        assertAuthHasInfo(captor.getValue(), user, hashedPassword);
        verify(dataSource).setUnlogged(user);
        verify(player).kickPlayer(argThat(containsString("please log in again")));
    }

    private void assertAuthHasInfo(PlayerAuth auth, String name, HashedPassword hashedPassword) {
        assertThat(auth.getRealName(), equalTo(name));
        assertThat(auth.getNickname(), equalTo(name.toLowerCase()));
        assertThat(auth.getPassword(), equalTo(hashedPassword));
    }
}
