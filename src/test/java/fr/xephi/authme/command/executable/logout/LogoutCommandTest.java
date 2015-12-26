package fr.xephi.authme.command.executable.logout;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.WrapperMock;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link LogoutCommand}.
 */
public class LogoutCommandTest {

    private CommandService commandService;

    @Before
    public void initializeAuthMeMock() {
        WrapperMock.createInstance();
        Settings.captchaLength = 10;
        commandService = mock(CommandService.class);
    }

    @Test
    public void shouldStopIfSenderIsNotAPlayer() {
        // given
        CommandSender sender = mock(BlockCommandSender.class);
        LogoutCommand command = new LogoutCommand();

        // when
        command.executeCommand(sender, new ArrayList<String>(), commandService);

        // then
        verify(commandService, never()).getManagement();
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue(), containsString("only for players"));
    }

    @Test
    public void shouldCallManagementForPlayerCaller() {
        // given
        Player sender = mock(Player.class);
        LogoutCommand command = new LogoutCommand();
        Management management = mock(Management.class);
        given(commandService.getManagement()).willReturn(management);

        // when
        command.executeCommand(sender, Collections.singletonList("password"), commandService);

        // then
        verify(management).performLogout(sender);
    }

}
