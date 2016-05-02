package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.cache.auth.PlayerAuth;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link GetEmailCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class GetEmailCommandTest {

    @InjectMocks
    private GetEmailCommand command;

    @Mock
    private DataSource dataSource;

    @Mock
    private CommandSender sender;

    @Mock
    private CommandService service;

    @Test
    public void shouldReportUnknownUser() {
        // given
        String user = "myTestUser";
        given(dataSource.getAuth(user)).willReturn(null);

        // when
        command.executeCommand(sender, Collections.singletonList(user), service);

        // then
        verify(service).send(sender, MessageKey.UNKNOWN_USER);
    }

    @Test
    public void shouldReturnEmail() {
        // given
        String user = "userToView";
        String email = "user.email@example.org";
        PlayerAuth auth = mock(PlayerAuth.class);
        given(auth.getEmail()).willReturn(email);
        given(dataSource.getAuth(user)).willReturn(auth);

        // when
        command.executeCommand(sender, Collections.singletonList(user), service);

        // then
        verify(sender).sendMessage(argThat(containsString(email)));
    }
}
