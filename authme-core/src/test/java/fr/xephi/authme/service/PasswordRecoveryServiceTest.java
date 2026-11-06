package fr.xephi.authme.service;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link PasswordRecoveryService}.
 */
@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {

    @InjectMocks
    private PasswordRecoveryService recoveryService;

    @Mock
    private CommonService commonService;

    @Mock
    private RecoveryCodeService codeService;

    @Mock
    private DataSource dataSource;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordSecurity passwordSecurity;

    @Mock
    private Messages messages;

    @BeforeAll
    static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @BeforeEach
    void runPostConstructMethod() {
        given(commonService.getProperty(SecuritySettings.EMAIL_RECOVERY_COOLDOWN_SECONDS)).willReturn(40);
        given(commonService.getProperty(SecuritySettings.PASSWORD_CHANGE_TIMEOUT)).willReturn(2);
        ReflectionTestUtils.invokePostConstructMethods(recoveryService);
    }

    @Test
    void shouldSendRecoveryCode() {
        // given
        Player player = mock(Player.class);
        String name = "Carl";
        given(player.getName()).willReturn(name);
        String email = "test@example.com";
        String code = "qwerty";
        given(codeService.generateCode(name)).willReturn(code);
        given(emailService.sendRecoveryCode(player.getName(), email, code)).willReturn(true);

        // when
        recoveryService.createAndSendRecoveryCode(player, email);

        // then
        verify(codeService).generateCode(name);
        verify(emailService).sendRecoveryCode(name, email, code);
        verify(commonService).send(player, MessageKey.RECOVERY_CODE_SENT);
    }

    @Test
    void shouldSendNewPasswordAndThenSaveIt() {
        // given
        Player player = mock(Player.class);
        String name = "Carl";
        given(player.getName()).willReturn(name);
        String email = "test@example.com";
        given(commonService.getProperty(EmailSettings.RECOVERY_PASSWORD_LENGTH)).willReturn(12);
        HashedPassword hash = new HashedPassword("new-hash");
        given(passwordSecurity.computeHash(anyString(), eq(name))).willReturn(hash);
        given(emailService.sendPasswordMail(eq(name), eq(email), anyString())).willReturn(true);
        given(dataSource.updatePassword(name, hash)).willReturn(true);

        // when
        recoveryService.generateAndSendNewPassword(player, email);

        // then
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        InOrder inOrder = inOrder(emailService, dataSource);
        inOrder.verify(emailService).sendPasswordMail(eq(name), eq(email), passwordCaptor.capture());
        inOrder.verify(dataSource).updatePassword(name, hash);
        verify(passwordSecurity).computeHash(passwordCaptor.getValue(), name);
        verify(commonService).send(player, MessageKey.RECOVERY_EMAIL_SENT_MESSAGE);
    }

    @Test
    void shouldNotChangePasswordIfNewPasswordMailCannotBeSent() {
        // given
        Player player = mock(Player.class);
        String name = "Carl";
        given(player.getName()).willReturn(name);
        String email = "test@example.com";
        given(commonService.getProperty(EmailSettings.RECOVERY_PASSWORD_LENGTH)).willReturn(12);
        given(passwordSecurity.computeHash(anyString(), eq(name))).willReturn(new HashedPassword("new-hash"));
        given(emailService.sendPasswordMail(eq(name), eq(email), anyString())).willReturn(false);

        // when
        recoveryService.generateAndSendNewPassword(player, email);

        // then
        verify(dataSource, never()).updatePassword(anyString(), any(HashedPassword.class));
        verify(commonService).send(player, MessageKey.EMAIL_SEND_FAILURE);
        verify(commonService, never()).send(player, MessageKey.RECOVERY_EMAIL_SENT_MESSAGE);
    }

    @Test
    void shouldReportErrorIfNewPasswordCannotBeSaved() {
        // given
        Player player = mock(Player.class);
        String name = "Carl";
        given(player.getName()).willReturn(name);
        String email = "test@example.com";
        given(commonService.getProperty(EmailSettings.RECOVERY_PASSWORD_LENGTH)).willReturn(12);
        HashedPassword hash = new HashedPassword("new-hash");
        given(passwordSecurity.computeHash(anyString(), eq(name))).willReturn(hash);
        given(emailService.sendPasswordMail(eq(name), eq(email), anyString())).willReturn(true);
        given(dataSource.updatePassword(name, hash)).willReturn(false);

        // when
        recoveryService.generateAndSendNewPassword(player, email);

        // then
        verify(commonService).send(player, MessageKey.ERROR);
        verify(commonService, never()).send(player, MessageKey.RECOVERY_EMAIL_SENT_MESSAGE);
    }

    @Test
    void shouldKeepTrackOfSuccessfulRecoversByIp() {
        // given
        Player bobby = mock(Player.class);
        TestHelper.mockIpAddressToPlayer(bobby, "192.168.8.8");
        given(bobby.getName()).willReturn("bobby");

        Player bobby2 = mock(Player.class);
        TestHelper.mockIpAddressToPlayer(bobby2, "127.0.0.1");
        given(bobby2.getName()).willReturn("bobby");

        Player other = mock(Player.class);
        TestHelper.mockIpAddressToPlayer(other, "192.168.8.8");
        given(other.getName()).willReturn("other");

        // when
        recoveryService.addSuccessfulRecovery(bobby);

        // then
        assertThat(recoveryService.canChangePassword(bobby), equalTo(true));
        assertThat(recoveryService.canChangePassword(bobby2), equalTo(false));
        assertThat(recoveryService.canChangePassword(other), equalTo(false));
    }

    @Test
    void shouldRemovePlayerFromSuccessfulRecovers() {
        // given
        Player bobby = mock(Player.class);
        TestHelper.mockIpAddressToPlayer(bobby, "192.168.8.8");
        given(bobby.getName()).willReturn("bobby");
        recoveryService.addSuccessfulRecovery(bobby);

        Player other = mock(Player.class);
        TestHelper.mockIpAddressToPlayer(other, "8.8.8.8");
        given(other.getName()).willReturn("other");
        recoveryService.addSuccessfulRecovery(other);

        // when
        recoveryService.removeFromSuccessfulRecovery(other);


        // then
        assertThat(recoveryService.canChangePassword(bobby), equalTo(true));
        assertThat(recoveryService.canChangePassword(other), equalTo(false));
    }
}
