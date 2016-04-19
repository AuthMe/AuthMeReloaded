package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link GetEmailCommand}.
 */
public class GetEmailCommandTest {

    @Test
    public void shouldReportUnknownUser() {
        // given
        String user = "myTestUser";
        DataSource dataSource = mock(DataSource.class);
        given(dataSource.getAuth(user)).willReturn(null);
        CommandService service = mock(CommandService.class);
        given(service.getDataSource()).willReturn(dataSource);

        CommandSender sender = mock(CommandSender.class);
        ExecutableCommand command = new GetEmailCommand();

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

        DataSource dataSource = mock(DataSource.class);
        given(dataSource.getAuth(user)).willReturn(auth);
        CommandService service = mock(CommandService.class);
        given(service.getDataSource()).willReturn(dataSource);

        CommandSender sender = mock(CommandSender.class);
        ExecutableCommand command = new GetEmailCommand();

        // when
        command.executeCommand(sender, Collections.singletonList(user), service);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender).sendMessage(captor.capture());
        assertThat(captor.getValue(), containsString(email));
    }
}
