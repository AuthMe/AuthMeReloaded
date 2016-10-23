package fr.xephi.authme.command.executable.login;

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
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link LoginCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class LoginCommandTest {

    @InjectMocks
    private LoginCommand command;

    @Mock
    private Management management;


    @Test
    public void shouldStopIfSenderIsNotAPlayer() {
        // given
        CommandSender sender = mock(BlockCommandSender.class);

        // when
        command.executeCommand(sender, new ArrayList<String>());

        // then
        verifyZeroInteractions(management);
        verify(sender).sendMessage(argThat(containsString("only for players")));
    }

    @Test
    public void shouldCallManagementForPlayerCaller() {
        // given
        Player sender = mock(Player.class);

        // when
        command.executeCommand(sender, Collections.singletonList("password"));

        // then
        verify(management).performLogin(eq(sender), eq("password"));
    }

}
