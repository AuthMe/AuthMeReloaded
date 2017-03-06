package fr.xephi.authme.service;

import ch.jalu.injector.testing.BeforeInjecting;
import ch.jalu.injector.testing.InjectDelayed;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link PasswordRecoveryService}.
 */
@Ignore
@RunWith(MockitoJUnitRunner.class)
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
    private RecoveryCodeService recoveryCodeService;

    @Mock
    private Messages messages;

    @BeforeInjecting
    public void initSettings() {
        given(commonService.getProperty(SecuritySettings.EMAIL_RECOVERY_COOLDOWN_SECONDS)).willReturn(40);
    }

    //TODO: Write tests
}
