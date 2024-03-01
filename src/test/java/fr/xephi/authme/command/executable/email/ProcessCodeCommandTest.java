package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.PasswordRecoveryService;
import fr.xephi.authme.service.RecoveryCodeService;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link ProcessCodeCommand}.
 */
@ExtendWith(MockitoExtension.class)
class ProcessCodeCommandTest {

    @InjectMocks
    private ProcessCodeCommand command;

    @Mock
    private CommonService commonService;

    @Mock
    private RecoveryCodeService codeService;

    @Mock
    private PasswordRecoveryService recoveryService;

    @Test
    void shouldSendErrorForInvalidRecoveryCode() {
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
    void shouldSendErrorForNoMoreTries() {
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
    void shouldProcessCorrectCode() {
        // given
        String name = "Dwight";
        String code = "chickenDinner";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(codeService.hasTriesLeft(name)).willReturn(true);
        given(codeService.isCodeValid(name, code)).willReturn(true);

        // when
        command.runCommand(sender, Collections.singletonList(code));

        // then
        verify(commonService).send(sender, MessageKey.RECOVERY_CODE_CORRECT);
        verify(recoveryService).addSuccessfulRecovery(sender);
        verify(codeService).removeCode(name);
    }
}
