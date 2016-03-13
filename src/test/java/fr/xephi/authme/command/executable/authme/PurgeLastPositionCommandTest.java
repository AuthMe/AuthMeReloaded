package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import org.bukkit.command.CommandSender;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link PurgeLastPositionCommand}.
 */
public class PurgeLastPositionCommandTest {

    @Test
    public void shouldPurgeLastPosOfUser() {
        // given
        String player = "_Bobby";
        PlayerAuth auth = mock(PlayerAuth.class);

        DataSource dataSource = mock(DataSource.class);
        given(dataSource.getAuth(player)).willReturn(auth);
        CommandService service = mock(CommandService.class);
        given(service.getDataSource()).willReturn(dataSource);

        CommandSender sender = mock(CommandSender.class);
        ExecutableCommand command = new PurgeLastPositionCommand();

        // when
        command.executeCommand(sender, Collections.singletonList(player), service);

        // then
        verify(dataSource).getAuth(player);
        verifyPositionWasReset(auth);
        verify(sender).sendMessage(argThat(containsString("last position location is now reset")));
    }

    @Test
    public void shouldPurgePositionOfCommandSender() {
        // given
        String player = "_Bobby";
        CommandSender sender = mock(CommandSender.class);
        given(sender.getName()).willReturn(player);

        PlayerAuth auth = mock(PlayerAuth.class);
        DataSource dataSource = mock(DataSource.class);
        given(dataSource.getAuth(player)).willReturn(auth);
        CommandService service = mock(CommandService.class);
        given(service.getDataSource()).willReturn(dataSource);

        ExecutableCommand command = new PurgeLastPositionCommand();

        // when
        command.executeCommand(sender, Collections.EMPTY_LIST, service);

        // then
        verify(dataSource).getAuth(player);
        verifyPositionWasReset(auth);
        verify(sender).sendMessage(argThat(containsString("position location is now reset")));
    }

    @Test
    public void shouldHandleNonExistentUser() {
        // given
        DataSource dataSource = mock(DataSource.class);
        CommandService service = mock(CommandService.class);
        given(service.getDataSource()).willReturn(dataSource);

        ExecutableCommand command = new PurgeLastPositionCommand();
        CommandSender sender = mock(CommandSender.class);
        String name = "invalidPlayer";

        // when
        command.executeCommand(sender, Collections.singletonList(name), service);

        // then
        verify(dataSource).getAuth(name);
        verify(service).send(sender, MessageKey.UNKNOWN_USER);
    }

    @Test
    public void shouldResetAllLastPositions() {
        // given
        PlayerAuth auth1 = mock(PlayerAuth.class);
        PlayerAuth auth2 = mock(PlayerAuth.class);
        PlayerAuth auth3 = mock(PlayerAuth.class);

        DataSource dataSource = mock(DataSource.class);
        given(dataSource.getAllAuths()).willReturn(Arrays.asList(auth1, auth2, auth3));
        CommandService service = mock(CommandService.class);
        given(service.getDataSource()).willReturn(dataSource);

        ExecutableCommand command = new PurgeLastPositionCommand();
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList("*"), service);

        // then
        verify(dataSource).getAllAuths();
        verifyPositionWasReset(auth1);
        verifyPositionWasReset(auth2);
        verifyPositionWasReset(auth3);
        verify(sender).sendMessage(argThat(containsString("last position locations are now reset")));
    }


    private static void verifyPositionWasReset(PlayerAuth auth) {
        verify(auth).setQuitLocX(0);
        verify(auth).setQuitLocY(0);
        verify(auth).setQuitLocZ(0);
        verify(auth).setWorld("world");
    }
}
