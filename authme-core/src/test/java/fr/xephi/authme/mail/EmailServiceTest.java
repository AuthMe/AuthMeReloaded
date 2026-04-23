package fr.xephi.authme.mail;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
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
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    private EmailService emailService;

    @Mock
    private Settings settings;
    @Mock
    private SendMailSsl sendMailSsl;
    @TempDir
    File dataFolder;

    @BeforeAll
    static void initLogger() {
        TestHelper.setupLogger();
    }

    @BeforeEach
    void initFieldsAndService() {
        emailService = new EmailService(dataFolder, settings, sendMailSsl);
    }

    @Test
    void shouldHaveAllInformation() {
        // given
        given(sendMailSsl.hasAllInformation()).willReturn(true);

        // when / then
        assertThat(emailService.hasAllInformation(), equalTo(true));
    }

    @Test
    void shouldSendPasswordMail() throws EmailException {
        // given
        given(settings.getPasswordEmailMessage())
            .willReturn("Hi <playername />, your new password for <servername /> is <generatedpass />");
        given(settings.getProperty(EmailSettings.PASSWORD_AS_IMAGE)).willReturn(false);
        given(settings.getProperty(PluginSettings.SERVER_NAME)).willReturn("serverName");
        HtmlEmail email = mock(HtmlEmail.class);
        given(sendMailSsl.hasAllInformation()).willReturn(true);
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
    void shouldHandleMailCreationError() throws EmailException {
        // given
        given(sendMailSsl.hasAllInformation()).willReturn(true);
        doThrow(EmailException.class).when(sendMailSsl).initializeMail(anyString());

        // when
        boolean result = emailService.sendPasswordMail("Player", "user@example.com", "new_password");

        // then
        assertThat(result, equalTo(false));
        verify(sendMailSsl).initializeMail("user@example.com");
        verify(sendMailSsl, never()).sendEmail(anyString(), any(HtmlEmail.class));
    }

    @Test
    void shouldHandleMailSendingFailure() throws EmailException {
        // given
        given(sendMailSsl.hasAllInformation()).willReturn(true);
        given(settings.getPasswordEmailMessage()).willReturn("Hi <playername />, your new pass is <generatedpass />");
        given(settings.getProperty(EmailSettings.PASSWORD_AS_IMAGE)).willReturn(false);
        given(settings.getProperty(PluginSettings.SERVER_NAME)).willReturn("serverName");
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
    void shouldSendRecoveryCode() throws EmailException {
        // given
        given(settings.getProperty(SecuritySettings.RECOVERY_CODE_HOURS_VALID)).willReturn(7);
        given(settings.getProperty(PluginSettings.SERVER_NAME)).willReturn("serverName");
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
    void shouldHandleMailCreationErrorForRecoveryCode() throws EmailException {
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
    void shouldHandleFailureToSendRecoveryCode() throws EmailException {
        // given
        given(settings.getProperty(SecuritySettings.RECOVERY_CODE_HOURS_VALID)).willReturn(7);
        given(settings.getProperty(PluginSettings.SERVER_NAME)).willReturn("Server? I barely know her!");
        given(settings.getRecoveryCodeEmailMessage()).willReturn("Hi <playername />, your code is <recoverycode /> for <servername />");
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
        assertThat(messageCaptor.getValue(), equalTo("Hi John, your code is 1DEF77 for Server? I barely know her!"));
    }
}
