package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLoggerTestInitializer;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import org.bukkit.command.CommandSender;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link ReloadCommand}.
 */
public class ReloadCommandTest {

    @BeforeClass
    public static void setUpLogger() {
        ConsoleLoggerTestInitializer.setupLogger();
    }

    @Test
    public void shouldReload() throws Exception {
        // given
        AuthMe authMe = mock(AuthMe.class);
        CommandService service = mock(CommandService.class);
        given(service.getAuthMe()).willReturn(authMe);
        CommandSender sender = mock(CommandSender.class);
        ExecutableCommand command = new ReloadCommand();

        // when
        command.executeCommand(sender, Collections.<String> emptyList(), service);

        // then
        verify(authMe).reload();
        verify(service).send(sender, MessageKey.CONFIG_RELOAD_SUCCESS);
    }

    @Test
    public void shouldHandleReloadError() throws Exception {
        // given
        AuthMe authMe = mock(AuthMe.class);
        doThrow(IllegalStateException.class).when(authMe).reload();
        CommandService service = mock(CommandService.class);
        given(service.getAuthMe()).willReturn(authMe);
        CommandSender sender = mock(CommandSender.class);
        ExecutableCommand command = new ReloadCommand();

        // when
        command.executeCommand(sender, Collections.<String> emptyList(), service);

        // then
        verify(authMe).reload();
        verify(sender).sendMessage(matches("Error occurred.*"));
        verify(authMe).stopOrUnload();
    }
}
