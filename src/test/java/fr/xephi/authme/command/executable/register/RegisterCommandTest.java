package fr.xephi.authme.command.executable.register;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.output.MessageKey;
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

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link RegisterCommand}.
 */
public class RegisterCommandTest {

    private CommandService commandService;

    @Before
    public void initializeAuthMeMock() {
        WrapperMock.createInstance();
        Settings.captchaLength = 10;
        commandService = mock(CommandService.class);
    }

    @Test
    public void shouldNotRunForNonPlayerSender() {
        // given
        CommandSender sender = mock(BlockCommandSender.class);
        RegisterCommand command = new RegisterCommand();

        // when
        command.executeCommand(sender, new ArrayList<String>(), commandService);

        // then
        verify(commandService, never()).getManagement();
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue(), containsString("Player only!"));
    }

    /*@Test
    public void shouldFailForEmptyArguments() {
        // given
        CommandSender sender = mock(Player.class);
        RegisterCommand command = new RegisterCommand();

        // when
        command.executeCommand(sender, new ArrayList<String>(), commandService);

        // then
        verify(commandService).send(sender, MessageKey.USAGE_REGISTER);
        verify(commandService, never()).getManagement();
    }*/

    /*@Test
    public void shouldForwardRegister() {
        // given
        Player sender = mock(Player.class);
        RegisterCommand command = new RegisterCommand();
        Management management = mock(Management.class);
        given(commandService.getManagement()).willReturn(management);

        // when
        command.executeCommand(sender, Collections.singletonList("password"), commandService);

        // then
        verify(management).performRegister(sender, "password", "");
    }*/

}
