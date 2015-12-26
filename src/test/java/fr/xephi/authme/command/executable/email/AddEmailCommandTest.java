package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.process.Management;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link AddEmailCommand}.
 */
public class AddEmailCommandTest {

    private CommandService commandService;

    @Before
    public void setUpMocks() {
        commandService = mock(CommandService.class);
    }

    @Test
    public void shouldRejectNonPlayerSender() {
        // given
        CommandSender sender = Mockito.mock(BlockCommandSender.class);
        AddEmailCommand command = new AddEmailCommand();

        // when
        command.executeCommand(sender, new ArrayList<String>(), commandService);

        // then
        verify(commandService, never()).getManagement();
    }

    @Test
    public void shouldForwardData() {
        // given
        Player sender = Mockito.mock(Player.class);
        AddEmailCommand command = new AddEmailCommand();
        Management management = mock(Management.class);
        given(commandService.getManagement()).willReturn(management);

        // when
        command.executeCommand(sender, Arrays.asList("mail@example", "other_example"), commandService);

        // then
        verify(management).performAddEmail(sender, "mail@example", "other_example");
    }

}
