package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.service.ValidationService.ValidationResult;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Locale;

import static fr.xephi.authme.service.BukkitServiceTestHelper.setBukkitServiceToRunTaskOptionallyAsync;
import static fr.xephi.authme.service.BukkitServiceTestHelper.setBukkitServiceToScheduleSyncTaskFromOptionallyAsyncTask;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link RegisterAdminCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class RegisterAdminCommandTest {

    @InjectMocks
    private RegisterAdminCommand command;

    @Mock
    private PasswordSecurity passwordSecurity;

    @Mock
    private DataSource dataSource;

    @Mock
    private BukkitService bukkitService;

    @Mock
    private CommonService commandService;

    @Mock
    private ValidationService validationService;

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldRejectInvalidPassword() {
        // given
        String user = "tester";
        String password = "myPassword";
        given(validationService.validatePassword(password, user))
            .willReturn(new ValidationResult(MessageKey.INVALID_PASSWORD_LENGTH));
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Arrays.asList(user, password));

        // then
        verify(validationService).validatePassword(password, user);
        verify(commandService).send(sender, MessageKey.INVALID_PASSWORD_LENGTH, new String[0]);
        verify(bukkitService, never()).runTaskAsynchronously(any(Runnable.class));
    }

    @Test
    public void shouldRejectAlreadyRegisteredAccount() {
        // given
        String user = "my_name55";
        String password = "@some-pass@";
        given(validationService.validatePassword(password, user)).willReturn(new ValidationResult());
        given(dataSource.isAuthAvailable(user)).willReturn(true);
        CommandSender sender = mock(CommandSender.class);
        setBukkitServiceToRunTaskOptionallyAsync(bukkitService);

        // when
        command.executeCommand(sender, Arrays.asList(user, password));

        // then
        verify(validationService).validatePassword(password, user);
        verify(commandService).send(sender, MessageKey.NAME_ALREADY_REGISTERED);
        verify(dataSource, never()).saveAuth(any(PlayerAuth.class));
    }

    @Test
    public void shouldHandleSavingError() {
        // given
        String user = "test-test";
        String password = "afdjhfkt";
        given(validationService.validatePassword(password, user)).willReturn(new ValidationResult());
        given(dataSource.isAuthAvailable(user)).willReturn(false);
        given(dataSource.saveAuth(any(PlayerAuth.class))).willReturn(false);
        HashedPassword hashedPassword = new HashedPassword("235sdf4w5udsgf");
        given(passwordSecurity.computeHash(password, user)).willReturn(hashedPassword);
        CommandSender sender = mock(CommandSender.class);
        setBukkitServiceToRunTaskOptionallyAsync(bukkitService);

        // when
        command.executeCommand(sender, Arrays.asList(user, password));

        // then
        verify(validationService).validatePassword(password, user);
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
        given(validationService.validatePassword(password, user)).willReturn(new ValidationResult());
        given(dataSource.isAuthAvailable(user)).willReturn(false);
        given(dataSource.saveAuth(any(PlayerAuth.class))).willReturn(true);
        HashedPassword hashedPassword = new HashedPassword("$aea2345EW235dfsa@#R%987048");
        given(passwordSecurity.computeHash(password, user)).willReturn(hashedPassword);
        given(bukkitService.getPlayerExact(user)).willReturn(null);
        CommandSender sender = mock(CommandSender.class);
        setBukkitServiceToRunTaskOptionallyAsync(bukkitService);

        // when
        command.executeCommand(sender, Arrays.asList(user, password));

        // then
        verify(validationService).validatePassword(password, user);
        verify(commandService).send(sender, MessageKey.REGISTER_SUCCESS);
        ArgumentCaptor<PlayerAuth> captor = ArgumentCaptor.forClass(PlayerAuth.class);
        verify(dataSource).saveAuth(captor.capture());
        assertAuthHasInfo(captor.getValue(), user, hashedPassword);
    }

    @Test
    public void shouldRegisterOnlinePlayer() {
        // given
        String user = "someone";
        String password = "Al1O3P49S5%";
        given(validationService.validatePassword(password, user)).willReturn(new ValidationResult());
        given(dataSource.isAuthAvailable(user)).willReturn(false);
        given(dataSource.saveAuth(any(PlayerAuth.class))).willReturn(true);
        HashedPassword hashedPassword = new HashedPassword("$aea2345EW235dfsa@#R%987048");
        given(passwordSecurity.computeHash(password, user)).willReturn(hashedPassword);
        Player player = mock(Player.class);
        given(bukkitService.getPlayerExact(user)).willReturn(player);
        String kickForAdminRegister = "Admin registered you -- log in again";
        given(commandService.retrieveSingleMessage(player, MessageKey.KICK_FOR_ADMIN_REGISTER)).willReturn(kickForAdminRegister);
        CommandSender sender = mock(CommandSender.class);
        setBukkitServiceToScheduleSyncTaskFromOptionallyAsyncTask(bukkitService);
        setBukkitServiceToRunTaskOptionallyAsync(bukkitService);

        // when
        command.executeCommand(sender, Arrays.asList(user, password));

        // then
        verify(validationService).validatePassword(password, user);
        verify(commandService).send(sender, MessageKey.REGISTER_SUCCESS);
        ArgumentCaptor<PlayerAuth> captor = ArgumentCaptor.forClass(PlayerAuth.class);
        verify(dataSource).saveAuth(captor.capture());
        assertAuthHasInfo(captor.getValue(), user, hashedPassword);
        verify(player).kickPlayer(kickForAdminRegister);
    }

    private void assertAuthHasInfo(PlayerAuth auth, String name, HashedPassword hashedPassword) {
        assertThat(auth.getRealName(), equalTo(name));
        assertThat(auth.getNickname(), equalTo(name.toLowerCase(Locale.ROOT)));
        assertThat(auth.getPassword(), equalTo(hashedPassword));
    }
}
