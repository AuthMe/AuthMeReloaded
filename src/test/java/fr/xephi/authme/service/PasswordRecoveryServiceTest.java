package fr.xephi.authme.service;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
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
