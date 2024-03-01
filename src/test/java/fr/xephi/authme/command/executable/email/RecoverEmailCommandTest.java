package fr.xephi.authme.command.executable.email;

import ch.jalu.datasourcecolumns.data.DataSourceValueImpl;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.PasswordRecoveryService;
import fr.xephi.authme.service.RecoveryCodeService;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Locale;

import static fr.xephi.authme.service.BukkitServiceTestHelper.setBukkitServiceToRunTaskAsynchronously;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test for {@link RecoverEmailCommand}.
 */
@ExtendWith(MockitoExtension.class)
class RecoverEmailCommandTest {

    private static final String DEFAULT_EMAIL = "your@email.com";

    @InjectMocks
    private RecoverEmailCommand command;

    @Mock
    private CommonService commonService;

    @Mock
    private DataSource dataSource;

    @Mock
    private PlayerCache playerCache;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordRecoveryService recoveryService;
    
    @Mock
    private RecoveryCodeService recoveryCodeService;

    @Mock
    private BukkitService bukkitService;

    @BeforeAll
    static void initLogger() {
        TestHelper.setupLogger();
    }

    @Test
    void shouldHandleMissingMailProperties() {
        // given
        given(emailService.hasAllInformation()).willReturn(false);
        Player sender = mock(Player.class);

        // when
        command.executeCommand(sender, Collections.singletonList("some@email.tld"));

        // then
        verify(commonService).send(sender, MessageKey.INCOMPLETE_EMAIL_SETTINGS);
        verifyNoInteractions(dataSource);
    }

    @Test
    void shouldShowErrorForAuthenticatedUser() {
        // given
        String name = "Bobby";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(emailService.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(true);

        // when
        command.executeCommand(sender, Collections.singletonList("bobby@example.org"));

        // then
        verify(emailService).hasAllInformation();
        verifyNoInteractions(dataSource);
        verify(commonService).send(sender, MessageKey.ALREADY_LOGGED_IN_ERROR);
    }

    @Test
    void shouldShowRegisterMessageForUnregisteredPlayer() {
        // given
        String name = "Player123";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(emailService.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(dataSource.getEmail(name)).willReturn(DataSourceValueImpl.unknownRow());

        // when
        command.executeCommand(sender, Collections.singletonList("someone@example.com"));

        // then
        verify(emailService).hasAllInformation();
        verify(dataSource).getEmail(name);
        verifyNoMoreInteractions(dataSource);
        verify(commonService).send(sender, MessageKey.USAGE_REGISTER);
    }

    @Test
    void shouldHandleDefaultEmail() {
        // given
        String name = "Tract0r";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(emailService.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(dataSource.getEmail(name)).willReturn(DataSourceValueImpl.of(DEFAULT_EMAIL));

        // when
        command.executeCommand(sender, Collections.singletonList(DEFAULT_EMAIL));

        // then
        verify(emailService).hasAllInformation();
        verify(dataSource).getEmail(name);
        verifyNoMoreInteractions(dataSource);
        verify(commonService).send(sender, MessageKey.INVALID_EMAIL);
    }

    @Test
    void shouldHandleInvalidEmailInput() {
        // given
        String name = "Rapt0r";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(emailService.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(dataSource.getEmail(name)).willReturn(DataSourceValueImpl.of("raptor@example.org"));

        // when
        command.executeCommand(sender, Collections.singletonList("wrong-email@example.com"));

        // then
        verify(emailService).hasAllInformation();
        verify(dataSource).getEmail(name);
        verifyNoMoreInteractions(dataSource);
        verify(commonService).send(sender, MessageKey.INVALID_EMAIL);
    }

    @Test
    void shouldGenerateRecoveryCode() {
        // given
        String name = "Vultur3";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(emailService.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        String email = "v@example.com";
        given(dataSource.getEmail(name)).willReturn(DataSourceValueImpl.of(email));
        given(recoveryCodeService.isRecoveryCodeNeeded()).willReturn(true);
        setBukkitServiceToRunTaskAsynchronously(bukkitService);

        // when
        command.executeCommand(sender, Collections.singletonList(email.toUpperCase(Locale.ROOT)));

        // then
        verify(recoveryService, only()).createAndSendRecoveryCode(sender, email);
    }

    @Test
    void shouldGenerateNewPasswordWithoutRecoveryCode() {
        // given
        String name = "Vultur3";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(emailService.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        String email = "vulture@example.com";
        given(dataSource.getEmail(name)).willReturn(DataSourceValueImpl.of(email));
        given(recoveryCodeService.isRecoveryCodeNeeded()).willReturn(false);
        setBukkitServiceToRunTaskAsynchronously(bukkitService);

        // when
        command.executeCommand(sender, Collections.singletonList(email));

        // then
        verify(recoveryService, only()).generateAndSendNewPassword(sender, email);
    }

    @Test
    void shouldDefineArgumentMismatchMessage() {
        // given / when / then
        assertThat(command.getArgumentsMismatchMessage(), equalTo(MessageKey.USAGE_RECOVER_EMAIL));
    }
}
