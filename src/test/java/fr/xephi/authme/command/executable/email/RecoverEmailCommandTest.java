package fr.xephi.authme.command.executable.email;

import ch.jalu.datasourcecolumns.data.DataSourceValueImpl;
import ch.jalu.injector.testing.BeforeInjecting;
import ch.jalu.injector.testing.DelayedInjectionRunner;
import ch.jalu.injector.testing.InjectDelayed;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.PasswordRecoveryService;
import fr.xephi.authme.service.RecoveryCodeService;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Locale;

import static fr.xephi.authme.service.BukkitServiceTestHelper.setBukkitServiceToRunTaskAsynchronously;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test for {@link RecoverEmailCommand}.
 */
@RunWith(DelayedInjectionRunner.class)
public class RecoverEmailCommandTest {

    private static final String DEFAULT_EMAIL = "your@email.com";

    @InjectDelayed
    private RecoverEmailCommand command;

    @Mock
    private PasswordSecurity passwordSecurity;

    @Mock
    private CommonService commonService;

    @Mock
    private DataSource dataSource;

    @Mock
    private PlayerCache playerCache;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordRecoveryService recoveryService;
    
    @Mock
    private RecoveryCodeService recoveryCodeService;

    @Mock
    private BukkitService bukkitService;

    @BeforeClass
    public static void initLogger() {
        TestHelper.setupLogger();
    }

    @BeforeInjecting
    public void initSettings() {
        given(commonService.getProperty(SecuritySettings.EMAIL_RECOVERY_COOLDOWN_SECONDS)).willReturn(40);
    }

    @Test
    public void shouldHandleMissingMailProperties() {
        // given
        given(emailService.hasAllInformation()).willReturn(false);
        Player sender = mock(Player.class);

        // when
        command.executeCommand(sender, Collections.singletonList("some@email.tld"));

        // then
        verify(commonService).send(sender, MessageKey.INCOMPLETE_EMAIL_SETTINGS);
        verifyNoInteractions(dataSource, passwordSecurity);
    }

    @Test
    public void shouldShowErrorForAuthenticatedUser() {
        // given
        String name = "Bobby";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(emailService.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(true);

        // when
        command.executeCommand(sender, Collections.singletonList("bobby@example.org"));

        // then
        verify(emailService).hasAllInformation();
        verifyNoInteractions(dataSource);
        verify(commonService).send(sender, MessageKey.ALREADY_LOGGED_IN_ERROR);
    }

    @Test
    public void shouldShowRegisterMessageForUnregisteredPlayer() {
        // given
        String name = "Player123";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(emailService.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(dataSource.getEmail(name)).willReturn(DataSourceValueImpl.unknownRow());

        // when
        command.executeCommand(sender, Collections.singletonList("someone@example.com"));

        // then
        verify(emailService).hasAllInformation();
        verify(dataSource).getEmail(name);
        verifyNoMoreInteractions(dataSource);
        verify(commonService).send(sender, MessageKey.USAGE_REGISTER);
    }

    @Test
    public void shouldHandleDefaultEmail() {
        // given
        String name = "Tract0r";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(emailService.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(dataSource.getEmail(name)).willReturn(DataSourceValueImpl.of(DEFAULT_EMAIL));

        // when
        command.executeCommand(sender, Collections.singletonList(DEFAULT_EMAIL));

        // then
        verify(emailService).hasAllInformation();
        verify(dataSource).getEmail(name);
        verifyNoMoreInteractions(dataSource);
        verify(commonService).send(sender, MessageKey.INVALID_EMAIL);
    }

    @Test
    public void shouldHandleInvalidEmailInput() {
        // given
        String name = "Rapt0r";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(emailService.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(dataSource.getEmail(name)).willReturn(DataSourceValueImpl.of("raptor@example.org"));

        // when
        command.executeCommand(sender, Collections.singletonList("wrong-email@example.com"));

        // then
        verify(emailService).hasAllInformation();
        verify(dataSource).getEmail(name);
        verifyNoMoreInteractions(dataSource);
        verify(commonService).send(sender, MessageKey.INVALID_EMAIL);
    }

    @Test
    public void shouldGenerateRecoveryCode() {
        // given
        String name = "Vultur3";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(emailService.hasAllInformation()).willReturn(true);
        given(emailService.sendRecoveryCode(anyString(), anyString(), anyString())).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        String email = "v@example.com";
        given(dataSource.getEmail(name)).willReturn(DataSourceValueImpl.of(email));
        String code = "a94f37";
        given(recoveryCodeService.isRecoveryCodeNeeded()).willReturn(true);
        given(recoveryCodeService.generateCode(name)).willReturn(code);
        setBukkitServiceToRunTaskAsynchronously(bukkitService);

        // when
        command.executeCommand(sender, Collections.singletonList(email.toUpperCase(Locale.ROOT)));

        // then
        verify(emailService).hasAllInformation();
        verify(dataSource).getEmail(name);
        verify(recoveryService).createAndSendRecoveryCode(sender, email);
    }

    @Test
    public void shouldGenerateNewPasswordWithoutRecoveryCode() {
        // given
        String name = "Vultur3";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(emailService.hasAllInformation()).willReturn(true);
        given(emailService.sendPasswordMail(anyString(), anyString(), anyString())).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        String email = "vulture@example.com";
        given(dataSource.getEmail(name)).willReturn(DataSourceValueImpl.of(email));
        given(recoveryCodeService.isRecoveryCodeNeeded()).willReturn(false);
        setBukkitServiceToRunTaskAsynchronously(bukkitService);

        // when
        command.executeCommand(sender, Collections.singletonList(email));

        // then
        verify(emailService).hasAllInformation();
        verify(dataSource).getEmail(name);
        verify(recoveryService).generateAndSendNewPassword(sender, email);
    }

    @Test
    public void shouldDefineArgumentMismatchMessage() {
        // given / when / then
        assertThat(command.getArgumentsMismatchMessage(), equalTo(MessageKey.USAGE_RECOVER_EMAIL));
    }
}
