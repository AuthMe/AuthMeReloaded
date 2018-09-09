package fr.xephi.authme.command.executable.verification;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.VerificationCodeManager;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.CommonService;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link VerificationCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class VerificationCommandTest {

    @InjectMocks
    private VerificationCommand command;

    @Mock
    private CommonService commonService;

    @Mock
    private VerificationCodeManager codeManager;

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldDetectIfMailHasASetup() {
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
    public void shouldRequireAndAcceptCode() {
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
    public void shouldRejectCode() {
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
    public void shouldRejectVerificationDueToExpiration() {
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
    public void shouldRejectVerificationDueToVerifiedIdentity() {
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
    public void shouldRejectVerificationDueToUndefinedEmail() {
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
    public void shouldDefineArgumentMismatchMessage() {
        // given / when / then
        assertThat(command.getArgumentsMismatchMessage(), equalTo(MessageKey.USAGE_VERIFICATION_CODE));
    }

    private static Player mockPlayerWithName(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }
}
