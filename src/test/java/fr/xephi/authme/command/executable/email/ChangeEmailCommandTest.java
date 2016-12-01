package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.process.Management;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link ChangeEmailCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ChangeEmailCommandTest {

    @InjectMocks
    private ChangeEmailCommand command;

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

        // when
        command.executeCommand(sender, Arrays.asList("new.mail@example.org", "old_mail@example.org"));

        // then
        verify(management).performChangeEmail(sender, "new.mail@example.org", "old_mail@example.org");
    }

}
