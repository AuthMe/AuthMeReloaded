package fr.xephi.authme.command.executable.verification;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.VerificationCodeManager;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.CommonService;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link VerificationCommand}.
 */
@ExtendWith(MockitoExtension.class)
class VerificationCommandTest {

    @InjectMocks
    private VerificationCommand command;

    @Mock
    private CommonService commonService;

    @Mock
    private VerificationCodeManager codeManager;

    @BeforeAll
    static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Test
    void shouldDetectIfMailHasASetup() {
        // given
        String name = "Alligator";
        Player player = mockPlayerWithName(name);
        given(codeManager.canSendMail()).willReturn(false);

        // when
        command.executeCommand(player, Collections.singletonList("code"));

        // then
        verify(commonService).send(player, MessageKey.INCOMPLETE_EMAIL_SETTINGS);
    }

    @Test
    void shouldRequireAndAcceptCode() {
        // given
        String name = "Duck";
        String code = "123932";
        Player player = mockPlayerWithName(name);
        given(codeManager.canSendMail()).willReturn(true);
        given(codeManager.isVerificationRequired(player)).willReturn(true);
        given(codeManager.isCodeRequired(name)).willReturn(true);
        given(codeManager.checkCode(name, code)).willReturn(true);

        // when
        command.executeCommand(player, Collections.singletonList(code));

        // then
        verify(codeManager).isVerificationRequired(player);
        verify(codeManager).isCodeRequired(name);
        verify(codeManager).checkCode(name, code);
        verify(commonService).send(player, MessageKey.VERIFICATION_CODE_VERIFIED);
    }

    @Test
    void shouldRejectCode() {
        // given
        String name = "Spider";
        String code = "98345222";   // more than 6 digits
        Player player = mockPlayerWithName(name);
        given(codeManager.canSendMail()).willReturn(true);
        given(codeManager.isVerificationRequired(player)).willReturn(true);
        given(codeManager.isCodeRequired(name)).willReturn(true);
        given(codeManager.checkCode(name, code)).willReturn(false);

        // when
        command.executeCommand(player, Collections.singletonList(code));

        // then
        verify(codeManager).isVerificationRequired(player);
        verify(codeManager).isCodeRequired(name);
        verify(codeManager).checkCode(name, code);
        verify(commonService).send(player, MessageKey.INCORRECT_VERIFICATION_CODE);
    }

    @Test
    void shouldRejectVerificationDueToExpiration() {
        // given
        String name = "Dog";
        String code = "131552";
        Player player = mockPlayerWithName(name);
        given(codeManager.canSendMail()).willReturn(true);
        given(codeManager.isVerificationRequired(player)).willReturn(true);
        given(codeManager.isCodeRequired(name)).willReturn(false);

        // when
        command.executeCommand(player, Collections.singletonList(code));

        // then
        verify(codeManager).isVerificationRequired(player);
        verify(codeManager).isCodeRequired(name);
        verify(commonService).send(player, MessageKey.VERIFICATION_CODE_EXPIRED);
    }

    @Test
    void shouldRejectVerificationDueToVerifiedIdentity() {
        // given
        String name = "Cow";
        String code = "973583";
        Player player = mockPlayerWithName(name);
        given(codeManager.canSendMail()).willReturn(true);
        given(codeManager.isVerificationRequired(player)).willReturn(false);
        given(codeManager.hasEmail(name)).willReturn(true);

        // when
        command.executeCommand(player, Collections.singletonList(code));

        // then
        verify(codeManager).isVerificationRequired(player);
        verify(codeManager).hasEmail(name);
        verify(commonService).send(player, MessageKey.VERIFICATION_CODE_ALREADY_VERIFIED);
    }

    @Test
    void shouldRejectVerificationDueToUndefinedEmail() {
        // given
        String name = "Frog";
        String code = "774543";
        Player player = mockPlayerWithName(name);
        given(codeManager.canSendMail()).willReturn(true);
        given(codeManager.isVerificationRequired(player)).willReturn(false);
        given(codeManager.hasEmail(name)).willReturn(false);

        // when
        command.executeCommand(player, Collections.singletonList(code));

        // then
        verify(codeManager).isVerificationRequired(player);
        verify(codeManager).hasEmail(name);
        verify(commonService).send(player, MessageKey.VERIFICATION_CODE_EMAIL_NEEDED);
        verify(commonService).send(player, MessageKey.ADD_EMAIL_MESSAGE);
    }

    @Test
    void shouldDefineArgumentMismatchMessage() {
        // given / when / then
        assertThat(command.getArgumentsMismatchMessage(), equalTo(MessageKey.USAGE_VERIFICATION_CODE));
    }

    private static Player mockPlayerWithName(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }
}
