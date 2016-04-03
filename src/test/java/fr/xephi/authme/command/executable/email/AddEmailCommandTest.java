package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.process.Management;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link AddEmailCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AddEmailCommandTest {

    @Mock
    private CommandService commandService;

    @Test
    public void shouldRejectNonPlayerSender() {
        // given
        CommandSender sender = mock(BlockCommandSender.class);
        AddEmailCommand command = new AddEmailCommand();

        // when
        command.executeCommand(sender, new ArrayList<String>(), commandService);

        // then
        verify(commandService, never()).getManagement();
    }

    @Test
    public void shouldForwardData() {
        // given
        Player sender = mock(Player.class);
        String email = "mail@example";
        given(commandService.validateEmail(email)).willReturn(true);
        Management management = mock(Management.class);
        given(commandService.getManagement()).willReturn(management);
        AddEmailCommand command = new AddEmailCommand();

        // when
        command.executeCommand(sender, Arrays.asList(email, email), commandService);

        // then
        verify(management).performAddEmail(sender, email);
    }

    @Test
    public void shouldFailForConfirmationMismatch() {
        // given
        Player sender = mock(Player.class);
        String email = "asdfasdf@example.com";
        given(commandService.validateEmail(email)).willReturn(true);
        AddEmailCommand command = new AddEmailCommand();

        // when
        command.executeCommand(sender, Arrays.asList(email, "wrongConf"), commandService);

        // then
        verify(commandService, never()).getManagement();
        verify(commandService).send(sender, MessageKey.CONFIRM_EMAIL_MESSAGE);
    }

}
