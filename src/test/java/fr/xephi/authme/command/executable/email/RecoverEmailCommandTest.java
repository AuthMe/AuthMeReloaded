package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.cache.auth.EmailRecoveryData;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.mail.SendMailSSL;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static fr.xephi.authme.AuthMeMatchers.stringWithLength;
import static fr.xephi.authme.util.Utils.MILLIS_PER_HOUR;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
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
    private CommandService commandService;

    @Mock
    private DataSource dataSource;

    @Mock
    private PlayerCache playerCache;

    @Mock
    private SendMailSSL sendMailSsl;

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
        given(dataSource.getEmailRecoveryData(name)).willReturn(null);

        // when
        command.executeCommand(sender, Collections.singletonList("someone@example.com"));

        // then
        verify(sendMailSsl).hasAllInformation();
        verify(dataSource).getEmailRecoveryData(name);
        verifyNoMoreInteractions(dataSource);
        verify(commandService).send(sender, MessageKey.REGISTER_EMAIL_MESSAGE);
    }

    @Test
    public void shouldHandleDefaultEmail() {
        // given
        String name = "Tract0r";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(sendMailSsl.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(dataSource.getEmailRecoveryData(name)).willReturn(newEmailRecoveryData(DEFAULT_EMAIL));

        // when
        command.executeCommand(sender, Collections.singletonList(DEFAULT_EMAIL));

        // then
        verify(sendMailSsl).hasAllInformation();
        verify(dataSource).getEmailRecoveryData(name);
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
        given(dataSource.getEmailRecoveryData(name)).willReturn(newEmailRecoveryData("raptor@example.org"));

        // when
        command.executeCommand(sender, Collections.singletonList("wrong-email@example.com"));

        // then
        verify(sendMailSsl).hasAllInformation();
        verify(dataSource).getEmailRecoveryData(name);
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
        given(playerCache.isAuthenticated(name)).willReturn(false);
        String email = "v@example.com";
        given(dataSource.getEmailRecoveryData(name)).willReturn(newEmailRecoveryData(email));
        int codeLength = 7;
        given(commandService.getProperty(SecuritySettings.RECOVERY_CODE_LENGTH)).willReturn(codeLength);
        int hoursValid = 12;
        given(commandService.getProperty(SecuritySettings.RECOVERY_CODE_HOURS_VALID)).willReturn(hoursValid);

        // when
        command.executeCommand(sender, Collections.singletonList(email.toUpperCase()));

        // then
        verify(sendMailSsl).hasAllInformation();
        verify(dataSource).getEmailRecoveryData(name);
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> expirationCaptor = ArgumentCaptor.forClass(Long.class);
        verify(dataSource).setRecoveryCode(eq(name), codeCaptor.capture(), expirationCaptor.capture());
        assertThat(codeCaptor.getValue(), stringWithLength(codeLength));
        // Check expiration with a tolerance
        assertThat(expirationCaptor.getValue() - System.currentTimeMillis(),
            allOf(lessThan(12L * MILLIS_PER_HOUR), greaterThan((long) (11.9 * MILLIS_PER_HOUR))));
        verify(sendMailSsl).sendRecoveryCode(name, email, codeCaptor.getValue());
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
        String code = "A6EF3AC8";
        EmailRecoveryData recoveryData = newEmailRecoveryData(email, code);
        given(dataSource.getEmailRecoveryData(name)).willReturn(recoveryData);
        given(commandService.getProperty(EmailSettings.RECOVERY_PASSWORD_LENGTH)).willReturn(20);
        given(passwordSecurity.computeHash(anyString(), eq(name)))
            .willAnswer(invocation -> new HashedPassword((String) invocation.getArguments()[0]));

        // when
        command.executeCommand(sender, Arrays.asList(email, "bogus"));

        // then
        verify(sendMailSsl).hasAllInformation();
        verify(dataSource, only()).getEmailRecoveryData(name);
        verify(sender).sendMessage(argThat(containsString("The recovery code is not correct")));
        verifyNoMoreInteractions(sendMailSsl);
    }

    @Test
    public void shouldResetPasswordAndSendEmail() {
        // given
        String name = "Vultur3";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(sendMailSsl.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        String email = "vulture@example.com";
        String code = "A6EF3AC8";
        EmailRecoveryData recoveryData = newEmailRecoveryData(email, code);
        given(dataSource.getEmailRecoveryData(name)).willReturn(recoveryData);
        given(commandService.getProperty(EmailSettings.RECOVERY_PASSWORD_LENGTH)).willReturn(20);
        given(passwordSecurity.computeHash(anyString(), eq(name)))
            .willAnswer(invocation -> new HashedPassword((String) invocation.getArguments()[0]));

        // when
        command.executeCommand(sender, Arrays.asList(email, code));

        // then
        verify(sendMailSsl).hasAllInformation();
        verify(dataSource).getEmailRecoveryData(name);
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordSecurity).computeHash(passwordCaptor.capture(), eq(name));
        String generatedPassword = passwordCaptor.getValue();
        assertThat(generatedPassword, stringWithLength(20));
        verify(dataSource).updatePassword(eq(name), any(HashedPassword.class));
        verify(dataSource).removeRecoveryCode(name);
        verify(sendMailSsl).sendPasswordMail(name, email, generatedPassword);
        verify(commandService).send(sender, MessageKey.RECOVERY_EMAIL_SENT_MESSAGE);
    }


    private static EmailRecoveryData newEmailRecoveryData(String email) {
        return new EmailRecoveryData(email, null, 0L);
    }

    private static EmailRecoveryData newEmailRecoveryData(String email, String code) {
        return new EmailRecoveryData(email, code, System.currentTimeMillis() + 10_000);
    }

}
