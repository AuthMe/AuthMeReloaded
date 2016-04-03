package fr.xephi.authme.command.executable.register;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.util.WrapperMock;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import static fr.xephi.authme.settings.properties.RestrictionSettings.ENABLE_PASSWORD_CONFIRMATION;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.argThat;
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
        verify(sender).sendMessage(argThat(containsString("Player only!")));
    }

    @Test
    public void shouldFailForEmptyArguments() {
        // given
        CommandSender sender = mock(Player.class);
        RegisterCommand command = new RegisterCommand();

        // when
        command.executeCommand(sender, new ArrayList<String>(), commandService);

        // then
        verify(commandService).send(sender, MessageKey.USAGE_REGISTER);
        verify(commandService, never()).getManagement();
    }

    @Test
    public void shouldForwardRegister() {
        // given
        Player sender = mock(Player.class);
        RegisterCommand command = new RegisterCommand();
        Management management = mock(Management.class);
        given(commandService.getManagement()).willReturn(management);
        given(commandService.getProperty(ENABLE_PASSWORD_CONFIRMATION)).willReturn(false);
        given(commandService.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)).willReturn(false);

        // when
        command.executeCommand(sender, Collections.singletonList("password"), commandService);

        // then
        verify(management).performRegister(sender, "password", "");
    }

}
