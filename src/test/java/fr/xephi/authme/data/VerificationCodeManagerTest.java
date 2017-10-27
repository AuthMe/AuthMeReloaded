package fr.xephi.authme.data;

import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DataSourceResult;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class VerificationCodeManagerTest {

    @Test
    public void shouldRequireVerification() {
        // given
        String player = "ILoveTests";
        String email = "ilovetests@test.com";
        Settings settings = mockSettings();
        DataSource dataSource = mock(DataSource.class);
        EmailService emailServiceVerified = mockEmailService(true);
        EmailService emailServiceDenied = mockEmailService(false);
        given(dataSource.getEmail(player)).willReturn(DataSourceResult.of(email));
        VerificationCodeManager codeManager1 = new VerificationCodeManager(settings, dataSource, emailServiceVerified);
        VerificationCodeManager codeManager2 = new VerificationCodeManager(settings, dataSource, emailServiceVerified);
        codeManager2.verify(player);
        VerificationCodeManager codeManager3 = new VerificationCodeManager(settings, dataSource, emailServiceDenied);

        // when
        boolean test1 = codeManager1.isVerificationRequired(player);
        boolean test2 = codeManager2.isVerificationRequired(player);
        boolean test3 = codeManager3.isVerificationRequired(player);

        // then
        assertThat(test1, equalTo(true));
        assertThat(test2, equalTo(false));
        assertThat(test3, equalTo(false));
    }

    @Test
    public void shouldGenerateCode() {
        // given
        String player = "ILoveTests";
        String email = "ilovetests@test.com";
        Settings settings = mockSettings();
        DataSource dataSource = mock(DataSource.class);
        EmailService emailServiceVerified = mockEmailService(true);
        given(dataSource.getEmail(player)).willReturn(DataSourceResult.of(email));
        VerificationCodeManager codeManager1 = new VerificationCodeManager(settings, dataSource, emailServiceVerified);
        VerificationCodeManager codeManager2 = new VerificationCodeManager(settings, dataSource, emailServiceVerified);
        codeManager2.codeExistOrGenerateNew(player);

        // when
        boolean test1 = codeManager1.codeExistOrGenerateNew(player);
        boolean test2 = codeManager2.codeExistOrGenerateNew(player);

        // then
        assertThat(test1, equalTo(true));
        assertThat(test2, equalTo(false));
    }

    @Test
    public void shouldRequireCode() {
        // given
        String player = "ILoveTests";
        String email = "ilovetests@test.com";
        Settings settings = mockSettings();
        DataSource dataSource = mock(DataSource.class);
        EmailService emailServiceVerified = mockEmailService(true);
        given(dataSource.getEmail(player)).willReturn(DataSourceResult.of(email));
        VerificationCodeManager codeManager1 = new VerificationCodeManager(settings, dataSource, emailServiceVerified);
        VerificationCodeManager codeManager2 = new VerificationCodeManager(settings, dataSource, emailServiceVerified);
        codeManager2.codeExistOrGenerateNew(player);

        // when
        boolean test1 = codeManager1.isCodeRequired(player);
        boolean test2 = codeManager2.isCodeRequired(player);

        // then
        assertThat(test1, equalTo(false));
        assertThat(test2, equalTo(true));
    }

    @Test
    public void shouldVerifyCode() {
        // given
        String player = "ILoveTests";
        String code = "193458";
        String email = "ilovetests@test.com";
        Settings settings = mockSettings();
        DataSource dataSource = mock(DataSource.class);
        EmailService emailServiceVerified = mockEmailService(true);
        given(dataSource.getEmail(player)).willReturn(DataSourceResult.of(email));
        VerificationCodeManager codeManager1 = new VerificationCodeManager(settings, dataSource, emailServiceVerified);
        VerificationCodeManager codeManager2 = new VerificationCodeManager(settings, dataSource, emailServiceVerified);
        codeManager1.codeExistOrGenerateNew(player);

        // when
        boolean test1 = codeManager1.checkCode(player, code);
        boolean test2 = codeManager2.checkCode(player, code);

        // then
        assertThat(test1, equalTo(false));
        assertThat(test2, equalTo(false));
    }

    private EmailService mockEmailService(boolean enabled) {
        EmailService emailService = mock(EmailService.class);
        given(emailService.hasAllInformation()).willReturn(enabled);
        return emailService;
    }

    private Settings mockSettings() {
        Settings settings = mock(Settings.class);
        given(settings.getProperty(SecuritySettings.VERIFICATION_CODE_EXPIRATION_MINUTES)).willReturn(1);
        return settings;
    }
}
