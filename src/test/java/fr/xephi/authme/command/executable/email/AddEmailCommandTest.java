package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.Management;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link AddEmailCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AddEmailCommandTest {

    @InjectMocks
    private AddEmailCommand command;

    @Mock
    private CommandService commandService;

    @Mock
    private Management management;

    @Test
    public void shouldRejectNonPlayerSender() {
        // given
        CommandSender sender = mock(BlockCommandSender.class);

        // when
        command.executeCommand(sender, new ArrayList<String>());

        // then
        verifyZeroInteractions(management);
    }

    @Test
    public void shouldForwardData() {
        // given
        Player sender = mock(Player.class);
        String email = "mail@example";
        given(commandService.validateEmail(email)).willReturn(true);

        // when
        command.executeCommand(sender, Arrays.asList(email, email));

        // then
        verify(management).performAddEmail(sender, email);
    }

    @Test
    public void shouldFailForConfirmationMismatch() {
        // given
        Player sender = mock(Player.class);
        String email = "asdfasdf@example.com";
        given(commandService.validateEmail(email)).willReturn(true);

        // when
        command.executeCommand(sender, Arrays.asList(email, "wrongConf"));

        // then
        verifyZeroInteractions(management);
        verify(commandService).send(sender, MessageKey.CONFIRM_EMAIL_MESSAGE);
    }

}
