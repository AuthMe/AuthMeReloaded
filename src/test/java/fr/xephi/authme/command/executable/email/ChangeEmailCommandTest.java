package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.process.Management;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link ChangeEmailCommand}.
 */
public class ChangeEmailCommandTest {

    private CommandService commandService;

    @Before
    public void setUpMocks() {
        commandService = mock(CommandService.class);
    }

    @Test
    public void shouldRejectNonPlayerSender() {
        // given
        CommandSender sender = mock(BlockCommandSender.class);
        ChangeEmailCommand command = new ChangeEmailCommand();

        // when
        command.executeCommand(sender, new ArrayList<String>(), commandService);

        // then
        verify(commandService, never()).getManagement();
    }

    @Test
    public void shouldForwardData() {
        // given
        Player sender = mock(Player.class);
        ChangeEmailCommand command = new ChangeEmailCommand();
        Management management = mock(Management.class);
        given(commandService.getManagement()).willReturn(management);

        // when
        command.executeCommand(sender, Arrays.asList("new.mail@example.org", "old_mail@example.org"), commandService);

        // then
        verify(management).performChangeEmail(sender, "new.mail@example.org", "old_mail@example.org");
    }

}
