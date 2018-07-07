package fr.xephi.authme.command.executable.authme;

import ch.jalu.datasourcecolumns.data.DataSourceValueImpl;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.CommonService;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

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
    private CommonService service;

    @Test
    public void shouldReportUnknownUser() {
        // given
        String user = "myTestUser";
        given(dataSource.getEmail(user)).willReturn(DataSourceValueImpl.unknownRow());
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(user));

        // then
        verify(service).send(sender, MessageKey.UNKNOWN_USER);
    }

    @Test
    public void shouldReturnEmail() {
        // given
        String user = "userToView";
        String email = "user.email@example.org";
        given(dataSource.getEmail(user)).willReturn(DataSourceValueImpl.of(email));
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(user));

        // then
        verify(sender).sendMessage(argThat(containsString(email)));
    }
}
