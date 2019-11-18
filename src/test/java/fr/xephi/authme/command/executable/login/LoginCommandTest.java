package fr.xephi.authme.command.executable.login;

import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.Management;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

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
        command.executeCommand(sender, Collections.emptyList());

        // then
        verifyNoInteractions(management);
        verify(sender).sendMessage(argThat(containsString("/authme forcelogin <player>")));
    }

    @Test
    public void shouldCallManagementForPlayerCaller() {
        // given
        Player sender = mock(Player.class);

        // when
        command.executeCommand(sender, Collections.singletonList("password"));

        // then
        verify(management).performLogin(sender, "password" );
    }

    @Test
    public void shouldDefineArgumentMismatchMessage() {
        // given / when / then
        assertThat(command.getArgumentsMismatchMessage(), equalTo(MessageKey.USAGE_LOGIN));
    }
}
