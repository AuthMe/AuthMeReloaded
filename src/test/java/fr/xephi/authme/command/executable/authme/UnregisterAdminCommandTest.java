package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test for {@link UnregisterAdminCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class UnregisterAdminCommandTest {

    @InjectMocks
    private UnregisterAdminCommand command;

    @Mock
    private DataSource dataSource;

    @Mock
    private CommandService commandService;

    @Test
    public void shouldHandleUnknownPlayer() {
        // given
        String user = "bobby";
        given(dataSource.isAuthAvailable(user)).willReturn(false);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(user));

        // then
        verify(dataSource).isAuthAvailable(user);
        verifyNoMoreInteractions(dataSource);
        verify(commandService).send(sender, MessageKey.UNKNOWN_USER);
    }

    @Test
    public void shouldHandleDatabaseError() {
        // given
        String user = "personaNonGrata";
        given(dataSource.isAuthAvailable(argThat(equalToIgnoringCase(user)))).willReturn(true);
        given(dataSource.removeAuth(argThat(equalToIgnoringCase(user)))).willReturn(false);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(user));

        // then
        verify(dataSource).isAuthAvailable(argThat(equalToIgnoringCase(user)));
        verify(dataSource).removeAuth(argThat(equalToIgnoringCase(user)));
        verifyNoMoreInteractions(dataSource);
        verify(commandService).send(sender, MessageKey.ERROR);
    }

}
