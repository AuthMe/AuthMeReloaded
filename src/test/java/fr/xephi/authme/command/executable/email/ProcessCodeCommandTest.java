package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.PasswordRecoveryService;
import fr.xephi.authme.service.RecoveryCodeService;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link ProcessCodeCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcessCodeCommandTest {

    @InjectMocks
    private ProcessCodeCommand command;

    @Mock
    private CommonService commonService;

    @Mock
    private DataSource dataSource;

    @Mock
    private RecoveryCodeService codeService;

    @Mock
    private PasswordRecoveryService recoveryService;

    private static final String DEFAULT_EMAIL = "your@email.com";

    @Test
    public void shouldSendErrorForInvalidRecoveryCode() {
        // given
        String name = "Vultur3";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(codeService.hasTriesLeft(name)).willReturn(true);
        given(codeService.isCodeValid(name, "bogus")).willReturn(false);
        given(codeService.getTriesLeft(name)).willReturn(2);

        // when
        command.executeCommand(sender, Collections.singletonList("bogus"));

        // then
        verify(commonService).send(sender, MessageKey.INCORRECT_RECOVERY_CODE, "2");
        verifyNoMoreInteractions(recoveryService);
    }

    @Test
    public void shouldSendErrorForNoMoreTries() {
        // given
        String name = "BobbY";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(codeService.hasTriesLeft(name)).willReturn(false);

        // when
        command.executeCommand(sender, Collections.singletonList("bogus"));

        // then
        verify(commonService).send(sender, MessageKey.RECOVERY_TRIES_EXCEEDED);
        verify(codeService).removeCode(name);
        verifyNoMoreInteractions(recoveryService);
    }

    @Test
    public void shouldHandleDefaultEmail() {
        // given
        String name = "Tract0r";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(dataSource.getAuth(name)).willReturn(newAuthWithEmail(DEFAULT_EMAIL));
        given(codeService.hasTriesLeft(name)).willReturn(true);
        given(codeService.isCodeValid(name, "actual")).willReturn(true);

        // when
        command.executeCommand(sender, Collections.singletonList("actual"));

        // then
        verify(dataSource).getAuth(name);
        verifyNoMoreInteractions(dataSource);
        verify(commonService).send(sender, MessageKey.INVALID_EMAIL);
    }

    @Test
    public void shouldGenerateAndSendPassword() {
        // given
        String name = "GenericName";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        String email = "ran-out@example.com";
        PlayerAuth auth = newAuthWithEmail(email);
        given(dataSource.getAuth(name)).willReturn(auth);
        given(codeService.hasTriesLeft(name)).willReturn(true);
        given(codeService.isCodeValid(name, "actual")).willReturn(true);

        // when
        command.executeCommand(sender, Collections.singletonList("actual"));

        // then
        verify(recoveryService).generateAndSendNewPassword(sender, email);
        verify(codeService).removeCode(name);
    }

    private static PlayerAuth newAuthWithEmail(String email) {
        return PlayerAuth.builder()
            .name("name")
            .email(email)
            .build();
    }
}
