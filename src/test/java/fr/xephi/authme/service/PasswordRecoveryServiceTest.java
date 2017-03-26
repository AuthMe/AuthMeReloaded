package fr.xephi.authme.service;

import ch.jalu.injector.testing.BeforeInjecting;
import ch.jalu.injector.testing.DelayedInjectionRunner;
import ch.jalu.injector.testing.InjectDelayed;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link PasswordRecoveryService}.
 */
@RunWith(DelayedInjectionRunner.class)
public class PasswordRecoveryServiceTest {

    @InjectDelayed
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

    @BeforeInjecting
    public void initSettings() {
        given(commonService.getProperty(SecuritySettings.EMAIL_RECOVERY_COOLDOWN_SECONDS)).willReturn(40);
        given(commonService.getProperty(SecuritySettings.PASSWORD_CHANGE_TIMEOUT)).willReturn(2);
    }

    @Test
    public void shouldSendRecoveryCode() {
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
}
