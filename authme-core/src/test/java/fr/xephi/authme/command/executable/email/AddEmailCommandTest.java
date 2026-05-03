package fr.xephi.authme.command.executable.email;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.ValidationService;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link AddEmailCommand}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class AddEmailCommandTest {

    @InjectMocks
    private AddEmailCommand command;

    @Mock
    private CommonService commandService;

    @Mock
    private Management management;

    @Mock
    private Messages messages;

    @Mock
    private ValidationService validationService;

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
    public void shouldForwardData() {
        // given
        Player sender = mock(Player.class);
        String email = "mail@example.com";
        given(validationService.validateEmail(email)).willReturn(true);

        // when
        command.executeCommand(sender, Arrays.asList(email, email));

        // then
        verify(management).performAddEmail(sender, email);
    }

    @Test
    public void shouldFailForInvalidEmailFormat() {
        // given
        Player sender = mock(Player.class);
        String email = "notanemail";
        given(validationService.validateEmail(email)).willReturn(false);

        // when
        command.executeCommand(sender, Arrays.asList(email, email));

        // then
        verifyNoInteractions(management);
        verify(commandService).send(sender, MessageKey.INVALID_EMAIL);
    }

    @Test
    public void shouldFailForConfirmationMismatch() {
        // given
        Player sender = mock(Player.class);
        String email = "asdfasdf@example.com";
        given(validationService.validateEmail(email)).willReturn(true);

        // when
        command.executeCommand(sender, Arrays.asList(email, "wrongConf"));

        // then
        verifyNoInteractions(management);
        verify(commandService).send(sender, MessageKey.CONFIRM_EMAIL_MESSAGE);
    }

    @Test
    public void shouldDefineArgumentMismatchMessage() {
        // given / when / then
        assertThat(command.getArgumentsMismatchMessage(), equalTo(MessageKey.USAGE_ADD_EMAIL));
    }
}


