package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.output.MessageKey;
import org.bukkit.command.CommandSender;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link ReloadCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ReloadCommandTest {

    @InjectMocks
    private ReloadCommand command;

    @Mock
    private AuthMe authMe;

    @Mock
    private CommandService service;

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldReload() throws Exception {
        // given
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.<String>emptyList(), service);

        // then
        verify(authMe).reload();
        verify(service).send(sender, MessageKey.CONFIG_RELOAD_SUCCESS);
    }

    @Test
    public void shouldHandleReloadError() throws Exception {
        // given
        doThrow(IllegalStateException.class).when(authMe).reload();
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.<String>emptyList(), service);

        // then
        verify(authMe).reload();
        verify(sender).sendMessage(matches("Error occurred.*"));
        verify(authMe).stopOrUnload();
    }
}
