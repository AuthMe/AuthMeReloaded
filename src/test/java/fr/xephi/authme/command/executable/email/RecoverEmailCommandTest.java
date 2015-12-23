package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.util.WrapperMock;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;

import static org.mockito.Mockito.mock;

/**
 * Test for {@link RecoverEmailCommand}.
 */
public class RecoverEmailCommandTest {

    @Before
    public void setUpMocks() {
        WrapperMock wrapper = WrapperMock.createInstance();
    }

    @Test
    @Ignore
    public void shouldRejectNonPlayerSender() {
        // given
        CommandSender sender = Mockito.mock(BlockCommandSender.class);
        RecoverEmailCommand command = new RecoverEmailCommand();

        // when
        command.executeCommand(sender, new ArrayList<String>(), mock(CommandService.class));

        // then
    }

    // TODO ljacqu 20151121: Expand tests. This command doesn't use a scheduler and has all of its
    // logic inside here.
}
