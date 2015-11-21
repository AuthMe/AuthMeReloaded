package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.AuthMeMockUtil;
import fr.xephi.authme.command.CommandParts;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test for {@link RecoverEmailCommand}.
 */
public class RecoverEmailCommandTest {

    @Before
    public void setUpMocks() {
        AuthMeMockUtil.mockAuthMeInstance();
    }

    @Test
    public void shouldRejectNonPlayerSender() {
        // given
        CommandSender sender = Mockito.mock(BlockCommandSender.class);
        RecoverEmailCommand command = new RecoverEmailCommand();

        // when
        command.executeCommand(sender, new CommandParts(), new CommandParts());

        // then
    }

    // TODO ljacqu 20151121: Expand tests. This command doesn't use a scheduler and has all of its
    // logic inside here.
}
