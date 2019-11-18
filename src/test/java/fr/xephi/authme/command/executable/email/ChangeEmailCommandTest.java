package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.data.VerificationCodeManager;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.CommonService;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ChangeEmailCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ChangeEmailCommandTest {

    @InjectMocks
    private ChangeEmailCommand command;

    @Mock
    private Management management;

    @Mock
    private CommonService commonService;

    @Mock
    private VerificationCodeManager codeManager;


    @Test
    public void shouldRejectNonPlayerSender() {
        // given
        CommandSender sender = mock(BlockCommandSender.class);

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verifyNoInteractions(management);
    }

    @Test
    public void shouldStopIfVerificationIsRequired() {
        // given
        String name = "Testeroni";
        Player player = initPlayerWithName(name);
        given(codeManager.isVerificationRequired(player)).willReturn(true);

        // when
        command.executeCommand(player, Arrays.asList("mail@example.org", "otherMail@example.com"));

        // then
        verify(codeManager).codeExistOrGenerateNew(name);
        verify(commonService).send(player, MessageKey.VERIFICATION_CODE_REQUIRED);
        verifyNoInteractions(management);
    }

    @Test
    public void shouldForwardData() {
        // given
        Player sender = initPlayerWithName("AmATest");
        given(codeManager.isVerificationRequired(sender)).willReturn(false);

        // when
        command.executeCommand(sender, Arrays.asList("new.mail@example.org", "old_mail@example.org"));

        // then
        verify(management).performChangeEmail(sender, "new.mail@example.org", "old_mail@example.org");
        verify(codeManager).isVerificationRequired(sender);
    }

    @Test
    public void shouldDefineArgumentMismatchMessage() {
        // given / when / then
        assertThat(command.getArgumentsMismatchMessage(), equalTo(MessageKey.USAGE_CHANGE_EMAIL));
    }

    private Player initPlayerWithName(String name) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        return player;
    }
}
