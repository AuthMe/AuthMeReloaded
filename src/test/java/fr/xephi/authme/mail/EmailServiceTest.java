package fr.xephi.authme.mail;

import ch.jalu.injector.testing.BeforeInjecting;
import ch.jalu.injector.testing.DelayedInjectionRunner;
import ch.jalu.injector.testing.InjectDelayed;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link EmailService}.
 */
@RunWith(DelayedInjectionRunner.class)
public class EmailServiceTest {

    @InjectDelayed
    private EmailService emailService;

    @Mock
    private Settings settings;
    @Mock
    private SendMailSsl sendMailSsl;
    @DataFolder
    private File dataFolder;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void initLogger() {
        TestHelper.setupLogger();
    }

    @BeforeInjecting
    public void initFields() throws IOException {
        dataFolder = temporaryFolder.newFolder();
        given(settings.getProperty(PluginSettings.SERVER_NAME)).willReturn("serverName");
        given(settings.getProperty(EmailSettings.MAIL_ACCOUNT)).willReturn("mail@example.org");
        given(settings.getProperty(EmailSettings.MAIL_PASSWORD)).willReturn("pass1234");
        given(sendMailSsl.hasAllInformation()).willReturn(true);
    }

    @Test
    public void shouldHaveAllInformation() {
        // given / when / then
        assertThat(emailService.hasAllInformation(), equalTo(true));
    }

    @Test
    public void shouldSendPasswordMail() throws EmailException {
        // given
        given(settings.getPasswordEmailMessage())
            .willReturn("Hi <playername />, your new password for <servername /> is <generatedpass />");
        given(settings.getProperty(EmailSettings.PASSWORD_AS_IMAGE)).willReturn(false);
        HtmlEmail email = mock(HtmlEmail.class);
        given(sendMailSsl.initializeMail(anyString())).willReturn(email);
        given(sendMailSsl.sendEmail(anyString(), eq(email))).willReturn(true);

        // when
        boolean result = emailService.sendPasswordMail("Player", "user@example.com", "new_password");

        // then
        assertThat(result, equalTo(true));
        verify(sendMailSsl).initializeMail("user@example.com");
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendMailSsl).sendEmail(messageCaptor.capture(), eq(email));
        assertThat(messageCaptor.getValue(),
            equalTo("Hi Player, your new password for serverName is new_password"));
    }

    @Test
    public void shouldHandleMailCreationError() throws EmailException {
        // given
        doThrow(EmailException.class).when(sendMailSsl).initializeMail(anyString());

        // when
        boolean result = emailService.sendPasswordMail("Player", "user@example.com", "new_password");

        // then
        assertThat(result, equalTo(false));
        verify(sendMailSsl).initializeMail("user@example.com");
        verify(sendMailSsl, never()).sendEmail(anyString(), any(HtmlEmail.class));
    }

    @Test
    public void shouldHandleMailSendingFailure() throws EmailException {
        // given
        given(settings.getPasswordEmailMessage()).willReturn("Hi <playername />, your new pass is <generatedpass />");
        given(settings.getProperty(EmailSettings.PASSWORD_AS_IMAGE)).willReturn(false);
        HtmlEmail email = mock(HtmlEmail.class);
        given(sendMailSsl.initializeMail(anyString())).willReturn(email);
        given(sendMailSsl.sendEmail(anyString(), any(HtmlEmail.class))).willReturn(false);

        // when
        boolean result = emailService.sendPasswordMail("bobby", "user@example.com", "myPassw0rd");

        // then
        assertThat(result, equalTo(false));
        verify(sendMailSsl).initializeMail("user@example.com");
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendMailSsl).sendEmail(messageCaptor.capture(), eq(email));
        assertThat(messageCaptor.getValue(), equalTo("Hi bobby, your new pass is myPassw0rd"));
    }

    @Test
    public void shouldSendRecoveryCode() throws EmailException {
        // given
        given(settings.getProperty(SecuritySettings.RECOVERY_CODE_HOURS_VALID)).willReturn(7);
        given(settings.getRecoveryCodeEmailMessage())
            .willReturn("Hi <playername />, your code on <servername /> is <recoverycode /> (valid <hoursvalid /> hours)");
        HtmlEmail email = mock(HtmlEmail.class);
        given(sendMailSsl.initializeMail(anyString())).willReturn(email);
        given(sendMailSsl.sendEmail(anyString(), any(HtmlEmail.class))).willReturn(true);

        // when
        boolean result = emailService.sendRecoveryCode("Timmy", "tim@example.com", "12C56A");

        // then
        assertThat(result, equalTo(true));
        verify(sendMailSsl).initializeMail("tim@example.com");
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendMailSsl).sendEmail(messageCaptor.capture(), eq(email));
        assertThat(messageCaptor.getValue(), equalTo("Hi Timmy, your code on serverName is 12C56A (valid 7 hours)"));
    }

    @Test
    public void shouldHandleMailCreationErrorForRecoveryCode() throws EmailException {
        // given
        given(sendMailSsl.initializeMail(anyString())).willThrow(EmailException.class);

        // when
        boolean result = emailService.sendRecoveryCode("Player", "player@example.org", "ABC1234");

        // then
        assertThat(result, equalTo(false));
        verify(sendMailSsl).initializeMail("player@example.org");
        verify(sendMailSsl, never()).sendEmail(anyString(), any(HtmlEmail.class));
    }

    @Test
    public void shouldHandleFailureToSendRecoveryCode() throws EmailException {
        // given
        given(settings.getProperty(SecuritySettings.RECOVERY_CODE_HOURS_VALID)).willReturn(7);
        given(settings.getRecoveryCodeEmailMessage()).willReturn("Hi <playername />, your code is <recoverycode />");
        EmailService sendMailSpy = spy(emailService);
        HtmlEmail email = mock(HtmlEmail.class);
        given(sendMailSsl.initializeMail(anyString())).willReturn(email);
        given(sendMailSsl.sendEmail(anyString(), any(HtmlEmail.class))).willReturn(false);

        // when
        boolean result = sendMailSpy.sendRecoveryCode("John", "user@example.com", "1DEF77");

        // then
        assertThat(result, equalTo(false));
        verify(sendMailSsl).initializeMail("user@example.com");
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendMailSsl).sendEmail(messageCaptor.capture(), eq(email));
        assertThat(messageCaptor.getValue(), equalTo("Hi John, your code is 1DEF77"));
    }

}
