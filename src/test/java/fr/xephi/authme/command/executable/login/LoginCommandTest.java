package fr.xephi.authme.command.executable.login;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.process.Management;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link LoginCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class LoginCommandTest {

    @Mock
    private CommandService commandService;

    @Test
    public void shouldStopIfSenderIsNotAPlayer() {
        // given
        CommandSender sender = mock(BlockCommandSender.class);
        LoginCommand command = new LoginCommand();

        // when
        command.executeCommand(sender, new ArrayList<String>(), commandService);

        // then
        verify(commandService, never()).getManagement();
        verify(sender).sendMessage(argThat(containsString("only for players")));
    }

    @Test
    public void shouldCallManagementForPlayerCaller() {
        // given
        Player sender = mock(Player.class);
        LoginCommand command = new LoginCommand();
        Management management = mock(Management.class);
        given(commandService.getManagement()).willReturn(management);

        // when
        command.executeCommand(sender, Collections.singletonList("password"), commandService);

        // then
        verify(management).performLogin(eq(sender), eq("password"), eq(false));
    }

}
