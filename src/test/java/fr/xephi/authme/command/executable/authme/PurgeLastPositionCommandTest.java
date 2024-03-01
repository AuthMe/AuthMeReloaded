package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.bungeecord.BungeeSender;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link PurgeLastPositionCommand}.
 */
@ExtendWith(MockitoExtension.class)
class PurgeLastPositionCommandTest {

    @InjectMocks
    private PurgeLastPositionCommand command;

    @Mock
    private DataSource dataSource;

    @Mock
    private CommonService service;

    @Mock
    private BungeeSender bungeeSender;

    @Test
    void shouldPurgeLastPosOfUser() {
        // given
        String player = "_Bobby";
        PlayerAuth auth = mock(PlayerAuth.class);
        given(dataSource.getAuth(player)).willReturn(auth);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(player));

        // then
        verify(dataSource).getAuth(player);
        verifyPositionWasReset(auth);
        verify(sender).sendMessage(argThat(containsString("last position location is now reset")));
    }

    @Test
    void shouldPurgePositionOfCommandSender() {
        // given
        String player = "_Bobby";
        CommandSender sender = mock(CommandSender.class);
        given(sender.getName()).willReturn(player);
        PlayerAuth auth = mock(PlayerAuth.class);
        given(dataSource.getAuth(player)).willReturn(auth);

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(dataSource).getAuth(player);
        verifyPositionWasReset(auth);
        verify(sender).sendMessage(argThat(containsString("position location is now reset")));
    }

    @Test
    void shouldHandleNonExistentUser() {
        // given
        String name = "invalidPlayer";
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(name));

        // then
        verify(dataSource).getAuth(name);
        verify(service).send(sender, MessageKey.UNKNOWN_USER);
    }

    @Test
    void shouldResetAllLastPositions() {
        // given
        PlayerAuth auth1 = mock(PlayerAuth.class);
        PlayerAuth auth2 = mock(PlayerAuth.class);
        PlayerAuth auth3 = mock(PlayerAuth.class);
        given(dataSource.getAllAuths()).willReturn(Arrays.asList(auth1, auth2, auth3));
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList("*"));

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
