package fr.xephi.authme.mail;

import ch.jalu.injector.testing.BeforeInjecting;
import ch.jalu.injector.testing.DelayedInjectionRunner;
import ch.jalu.injector.testing.InjectDelayed;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.bukkit.Server;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link SendMailSSL}.
 */
@RunWith(DelayedInjectionRunner.class)
public class SendMailSSLTest {

    @InjectDelayed
    private SendMailSSL sendMailSSL;

    @Mock
    private Settings settings;
    @Mock
    private Server server;
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
        given(server.getServerName()).willReturn("serverName");
        given(settings.getProperty(EmailSettings.MAIL_ACCOUNT)).willReturn("mail@example.org");
        given(settings.getProperty(EmailSettings.MAIL_PASSWORD)).willReturn("pass1234");
    }

    @Test
    public void shouldHaveAllInformation() {
        // given / when / then
        assertThat(sendMailSSL.hasAllInformation(), equalTo(true));
    }

    @Test
    public void shouldSendPasswordMail() throws EmailException {
        // given
        given(settings.getPasswordEmailMessage())
            .willReturn("Hi <playername />, your new password for <servername /> is <generatedpass />");
        given(settings.getProperty(EmailSettings.PASSWORD_AS_IMAGE)).willReturn(false);
        SendMailSSL sendMailSpy = spy(sendMailSSL);
        HtmlEmail email = mock(HtmlEmail.class);
        doReturn(email).when(sendMailSpy).initializeMail(anyString());
        doReturn(true).when(sendMailSpy).sendEmail(anyString(), any(HtmlEmail.class));

        // when
        boolean result = sendMailSpy.sendPasswordMail("Player", "user@example.com", "new_password");

        // then
        assertThat(result, equalTo(true));
        verify(sendMailSpy).initializeMail("user@example.com");
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendMailSpy).sendEmail(messageCaptor.capture(), eq(email));
        assertThat(messageCaptor.getValue(),
            equalTo("Hi Player, your new password for serverName is new_password"));
    }

    @Test
    public void shouldHandleMailCreationError() throws EmailException {
        // given
        SendMailSSL sendMailSpy = spy(sendMailSSL);
        doThrow(EmailException.class).when(sendMailSpy).initializeMail(anyString());

        // when
        boolean result = sendMailSpy.sendPasswordMail("Player", "user@example.com", "new_password");

        // then
        assertThat(result, equalTo(false));
        verify(sendMailSpy).initializeMail("user@example.com");
        verify(sendMailSpy, never()).sendEmail(anyString(), any(HtmlEmail.class));
    }

    @Test
    public void shouldHandleMailSendingFailure() throws EmailException {
        // given
        given(settings.getPasswordEmailMessage()).willReturn("Hi <playername />, your new pass is <generatedpass />");
        given(settings.getProperty(EmailSettings.PASSWORD_AS_IMAGE)).willReturn(false);
        SendMailSSL sendMailSpy = spy(sendMailSSL);
        HtmlEmail email = mock(HtmlEmail.class);
        doReturn(email).when(sendMailSpy).initializeMail(anyString());
        doReturn(false).when(sendMailSpy).sendEmail(anyString(), any(HtmlEmail.class));

        // when
        boolean result = sendMailSpy.sendPasswordMail("bobby", "user@example.com", "myPassw0rd");

        // then
        assertThat(result, equalTo(false));
        verify(sendMailSpy).initializeMail("user@example.com");
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendMailSpy).sendEmail(messageCaptor.capture(), eq(email));
        assertThat(messageCaptor.getValue(), equalTo("Hi bobby, your new pass is myPassw0rd"));
    }

    @Test
    public void shouldSendRecoveryCode() throws EmailException {
        // given
        given(settings.getProperty(SecuritySettings.RECOVERY_CODE_HOURS_VALID)).willReturn(7);
        given(settings.getRecoveryCodeEmailMessage())
            .willReturn("Hi <playername />, your code on <servername /> is <recoverycode /> (valid <hoursvalid /> hours)");
        SendMailSSL sendMailSpy = spy(sendMailSSL);
        HtmlEmail email = mock(HtmlEmail.class);
        doReturn(email).when(sendMailSpy).initializeMail(anyString());
        doReturn(true).when(sendMailSpy).sendEmail(anyString(), any(HtmlEmail.class));

        // when
        boolean result = sendMailSpy.sendRecoveryCode("Timmy", "tim@example.com", "12C56A");

        // then
        assertThat(result, equalTo(true));
        verify(sendMailSpy).initializeMail("tim@example.com");
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendMailSpy).sendEmail(messageCaptor.capture(), eq(email));
        assertThat(messageCaptor.getValue(), equalTo("Hi Timmy, your code on serverName is 12C56A (valid 7 hours)"));
    }

    @Test
    public void shouldHandleMailCreationErrorForRecoveryCode() throws EmailException {
        // given
        SendMailSSL sendMailSpy = spy(sendMailSSL);
        doThrow(EmailException.class).when(sendMailSpy).initializeMail(anyString());

        // when
        boolean result = sendMailSpy.sendRecoveryCode("Player", "player@example.org", "ABC1234");

        // then
        assertThat(result, equalTo(false));
        verify(sendMailSpy).initializeMail("player@example.org");
        verify(sendMailSpy, never()).sendEmail(anyString(), any(HtmlEmail.class));
    }

    @Test
    public void shouldHandleFailureToSendRecoveryCode() throws EmailException {
        // given
        given(settings.getProperty(SecuritySettings.RECOVERY_CODE_HOURS_VALID)).willReturn(7);
        given(settings.getRecoveryCodeEmailMessage()).willReturn("Hi <playername />, your code is <recoverycode />");
        SendMailSSL sendMailSpy = spy(sendMailSSL);
        HtmlEmail email = mock(HtmlEmail.class);
        doReturn(email).when(sendMailSpy).initializeMail(anyString());
        doReturn(false).when(sendMailSpy).sendEmail(anyString(), any(HtmlEmail.class));

        // when
        boolean result = sendMailSpy.sendRecoveryCode("John", "user@example.com", "1DEF77");

        // then
        assertThat(result, equalTo(false));
        verify(sendMailSpy).initializeMail("user@example.com");
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendMailSpy).sendEmail(messageCaptor.capture(), eq(email));
        assertThat(messageCaptor.getValue(), equalTo("Hi John, your code is 1DEF77"));
    }

    @Test
    public void shouldCreateEmailObject() throws EmailException {
        // given
        given(settings.getProperty(EmailSettings.SMTP_PORT)).willReturn(465);
        String smtpHost = "mail.example.com";
        given(settings.getProperty(EmailSettings.SMTP_HOST)).willReturn(smtpHost);
        String senderMail = "sender@example.org";
        given(settings.getProperty(EmailSettings.MAIL_ACCOUNT)).willReturn(senderMail);
        String senderName = "Server administration";
        given(settings.getProperty(EmailSettings.MAIL_SENDER_NAME)).willReturn(senderName);

        // when
        HtmlEmail email = sendMailSSL.initializeMail("recipient@example.com");

        // then
        assertThat(email, not(nullValue()));
        assertThat(email.getToAddresses(), hasSize(1));
        assertThat(email.getToAddresses().get(0).getAddress(), equalTo("recipient@example.com"));
        assertThat(email.getFromAddress().getAddress(), equalTo(senderMail));
        assertThat(email.getFromAddress().getPersonal(), equalTo(senderName));
        assertThat(email.getHostName(), equalTo(smtpHost));
        assertThat(email.getSmtpPort(), equalTo("465"));
    }

    @Test
    public void shouldCreateEmailObjectWithOAuth2() throws EmailException {
        // given
        given(settings.getProperty(EmailSettings.SMTP_PORT)).willReturn(587);
        given(settings.getProperty(EmailSettings.OAUTH2_TOKEN)).willReturn("oAuth2 token");
        String smtpHost = "mail.example.com";
        given(settings.getProperty(EmailSettings.SMTP_HOST)).willReturn(smtpHost);
        String senderMail = "sender@example.org";
        given(settings.getProperty(EmailSettings.MAIL_ACCOUNT)).willReturn(senderMail);

        // when
        HtmlEmail email = sendMailSSL.initializeMail("recipient@example.com");

        // then
        assertThat(email, not(nullValue()));
        assertThat(email.getToAddresses(), hasSize(1));
        assertThat(email.getToAddresses().get(0).getAddress(), equalTo("recipient@example.com"));
        assertThat(email.getFromAddress().getAddress(), equalTo(senderMail));
        assertThat(email.getHostName(), equalTo(smtpHost));
        assertThat(email.getSmtpPort(), equalTo("587"));

        Properties mailProperties = email.getMailSession().getProperties();
        assertThat(mailProperties.getProperty("mail.smtp.auth.mechanisms"), equalTo("XOAUTH2"));
        assertThat(mailProperties.getProperty("mail.smtp.auth.plain.disable"), equalTo("true"));
        assertThat(mailProperties.getProperty(OAuth2SaslClientFactory.OAUTH_TOKEN_PROP), equalTo("oAuth2 token"));
    }

}
