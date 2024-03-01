package fr.xephi.authme.command.executable.logout;

import fr.xephi.authme.process.Management;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link LogoutCommand}.
 */
@ExtendWith(MockitoExtension.class)
class LogoutCommandTest {

    @InjectMocks
    private LogoutCommand command;

    @Mock
    private Management management;


    @Test
    void shouldStopIfSenderIsNotAPlayer() {
        // given
        CommandSender sender = mock(BlockCommandSender.class);

        // when
        command.executeCommand(sender, new ArrayList<String>());

        // then
        verifyNoInteractions(management);
        verify(sender).sendMessage(argThat(containsString("only for players")));
    }

    @Test
    void shouldCallManagementForPlayerCaller() {
        // given
        Player sender = mock(Player.class);

        // when
        command.executeCommand(sender, Collections.singletonList("password"));

        // then
        verify(management).performLogout(sender);
    }

}
