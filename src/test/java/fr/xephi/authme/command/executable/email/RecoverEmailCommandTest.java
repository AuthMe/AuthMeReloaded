package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.mail.SendMailSSL;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.RecoveryCodeService;
import fr.xephi.authme.settings.properties.EmailSettings;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static fr.xephi.authme.AuthMeMatchers.stringWithLength;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link RecoverEmailCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class RecoverEmailCommandTest {

    private static final String DEFAULT_EMAIL = "your@email.com";

    @InjectMocks
    private RecoverEmailCommand command;

    @Mock
    private PasswordSecurity passwordSecurity;

    @Mock
    private CommonService commandService;

    @Mock
    private DataSource dataSource;

    @Mock
    private PlayerCache playerCache;

    @Mock
    private SendMailSSL sendMailSsl;
    
    @Mock
    private RecoveryCodeService recoveryCodeService;

    @BeforeClass
    public static void initLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldHandleMissingMailProperties() {
        // given
        given(sendMailSsl.hasAllInformation()).willReturn(false);
        Player sender = mock(Player.class);

        // when
        command.executeCommand(sender, Collections.singletonList("some@email.tld"));

        // then
        verify(commandService).send(sender, MessageKey.INCOMPLETE_EMAIL_SETTINGS);
        verifyZeroInteractions(dataSource, passwordSecurity);
    }

    @Test
    public void shouldShowErrorForAuthenticatedUser() {
        // given
        String name = "Bobby";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(sendMailSsl.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(true);

        // when
        command.executeCommand(sender, Collections.singletonList("bobby@example.org"));

        // then
        verify(sendMailSsl).hasAllInformation();
        verifyZeroInteractions(dataSource);
        verify(commandService).send(sender, MessageKey.ALREADY_LOGGED_IN_ERROR);
    }

    @Test
    public void shouldShowRegisterMessageForUnregisteredPlayer() {
        // given
        String name = "Player123";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(sendMailSsl.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(dataSource.getAuth(name)).willReturn(null);

        // when
        command.executeCommand(sender, Collections.singletonList("someone@example.com"));

        // then
        verify(sendMailSsl).hasAllInformation();
        verify(dataSource).getAuth(name);
        verifyNoMoreInteractions(dataSource);
        verify(commandService).send(sender, MessageKey.USAGE_REGISTER);
    }

    @Test
    public void shouldHandleDefaultEmail() {
        // given
        String name = "Tract0r";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(sendMailSsl.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(dataSource.getAuth(name)).willReturn(newAuthWithEmail(DEFAULT_EMAIL));

        // when
        command.executeCommand(sender, Collections.singletonList(DEFAULT_EMAIL));

        // then
        verify(sendMailSsl).hasAllInformation();
        verify(dataSource).getAuth(name);
        verifyNoMoreInteractions(dataSource);
        verify(commandService).send(sender, MessageKey.INVALID_EMAIL);
    }

    @Test
    public void shouldHandleInvalidEmailInput() {
        // given
        String name = "Rapt0r";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(sendMailSsl.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(dataSource.getAuth(name)).willReturn(newAuthWithEmail("raptor@example.org"));

        // when
        command.executeCommand(sender, Collections.singletonList("wrong-email@example.com"));

        // then
        verify(sendMailSsl).hasAllInformation();
        verify(dataSource).getAuth(name);
        verifyNoMoreInteractions(dataSource);
        verify(commandService).send(sender, MessageKey.INVALID_EMAIL);
    }

    @Test
    public void shouldGenerateRecoveryCode() {
        // given
        String name = "Vultur3";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(sendMailSsl.hasAllInformation()).willReturn(true);
        given(sendMailSsl.sendRecoveryCode(anyString(), anyString(), anyString())).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        String email = "v@example.com";
        given(dataSource.getAuth(name)).willReturn(newAuthWithEmail(email));
        String code = "a94f37";
        given(recoveryCodeService.isRecoveryCodeNeeded()).willReturn(true);
        given(recoveryCodeService.generateCode(name)).willReturn(code);

        // when
        command.executeCommand(sender, Collections.singletonList(email.toUpperCase()));

        // then
        verify(sendMailSsl).hasAllInformation();
        verify(dataSource).getAuth(name);
        verify(recoveryCodeService).generateCode(name);
        verify(commandService).send(sender, MessageKey.RECOVERY_CODE_SENT);
        verify(sendMailSsl).sendRecoveryCode(name, email, code);
    }

    @Test
    public void shouldSendErrorForInvalidRecoveryCode() {
        // given
        String name = "Vultur3";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(sendMailSsl.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        String email = "vulture@example.com";
        PlayerAuth auth = newAuthWithEmail(email);
        given(dataSource.getAuth(name)).willReturn(auth);
        given(recoveryCodeService.isRecoveryCodeNeeded()).willReturn(true);
        given(recoveryCodeService.isCodeValid(name, "bogus")).willReturn(false);

        // when
        command.executeCommand(sender, Arrays.asList(email, "bogus"));

        // then
        verify(sendMailSsl).hasAllInformation();
        verify(dataSource, only()).getAuth(name);
        verify(commandService).send(sender, MessageKey.INCORRECT_RECOVERY_CODE);
        verifyNoMoreInteractions(sendMailSsl);
    }

    @Test
    public void shouldResetPasswordAndSendEmail() {
        // given
        String name = "Vultur3";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(sendMailSsl.hasAllInformation()).willReturn(true);
        given(sendMailSsl.sendPasswordMail(anyString(), anyString(), anyString())).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        String email = "vulture@example.com";
        String code = "A6EF3AC8";
        PlayerAuth auth = newAuthWithEmail(email);
        given(dataSource.getAuth(name)).willReturn(auth);
        given(commandService.getProperty(EmailSettings.RECOVERY_PASSWORD_LENGTH)).willReturn(20);
        given(passwordSecurity.computeHash(anyString(), eq(name)))
            .willAnswer(invocation -> new HashedPassword(invocation.getArgument(0)));
        given(recoveryCodeService.isRecoveryCodeNeeded()).willReturn(true);
        given(recoveryCodeService.isCodeValid(name, code)).willReturn(true);

        // when
        command.executeCommand(sender, Arrays.asList(email, code));

        // then
        verify(sendMailSsl).hasAllInformation();
        verify(dataSource).getAuth(name);
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordSecurity).computeHash(passwordCaptor.capture(), eq(name));
        String generatedPassword = passwordCaptor.getValue();
        assertThat(generatedPassword, stringWithLength(20));
        verify(dataSource).updatePassword(eq(name), any(HashedPassword.class));
        verify(recoveryCodeService).removeCode(name);
        verify(sendMailSsl).sendPasswordMail(name, email, generatedPassword);
        verify(commandService).send(sender, MessageKey.RECOVERY_EMAIL_SENT_MESSAGE);
    }

    @Test
    public void shouldGenerateNewPasswordWithoutRecoveryCode() {
        // given
        String name = "sh4rK";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(sendMailSsl.hasAllInformation()).willReturn(true);
        given(sendMailSsl.sendPasswordMail(anyString(), anyString(), anyString())).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        String email = "shark@example.org";
        PlayerAuth auth = newAuthWithEmail(email);
        given(dataSource.getAuth(name)).willReturn(auth);
        given(commandService.getProperty(EmailSettings.RECOVERY_PASSWORD_LENGTH)).willReturn(20);
        given(passwordSecurity.computeHash(anyString(), eq(name)))
            .willAnswer(invocation -> new HashedPassword(invocation.getArgument(0)));
        given(recoveryCodeService.isRecoveryCodeNeeded()).willReturn(false);

        // when
        command.executeCommand(sender, Collections.singletonList(email));

        // then
        verify(sendMailSsl).hasAllInformation();
        verify(dataSource).getAuth(name);
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordSecurity).computeHash(passwordCaptor.capture(), eq(name));
        String generatedPassword = passwordCaptor.getValue();
        assertThat(generatedPassword, stringWithLength(20));
        verify(dataSource).updatePassword(eq(name), any(HashedPassword.class));
        verify(sendMailSsl).sendPasswordMail(name, email, generatedPassword);
        verify(commandService).send(sender, MessageKey.RECOVERY_EMAIL_SENT_MESSAGE);
    }


    private static PlayerAuth newAuthWithEmail(String email) {
        return PlayerAuth.builder()
            .name("name")
            .email(email)
            .build();
    }
}
