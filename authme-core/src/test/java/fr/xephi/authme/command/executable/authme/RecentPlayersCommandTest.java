package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link RecentPlayersCommand}.
 */
@ExtendWith(MockitoExtension.class)
class RecentPlayersCommandTest {

    @InjectMocks
    @Spy
    private RecentPlayersCommand command;

    @Mock
    private DataSource dataSource;

    @Test
    void shouldShowRecentPlayers() {
        // given
        PlayerAuth auth1 = PlayerAuth.builder()
            .name("hannah").realName("Hannah").lastIp("11.11.11.11")
            .lastLogin(1510387755000L) // 11/11/2017 @ 8:09am
            .build();
        PlayerAuth auth2 = PlayerAuth.builder()
            .name("matt").realName("MATT").lastIp("22.11.22.33")
            .lastLogin(1510269301000L) // 11/09/2017 @ 11:15pm
            .build();
        doReturn(ZoneId.of("UTC")).when(command).getZoneId();
        given(dataSource.getRecentlyLoggedInPlayers()).willReturn(Arrays.asList(auth1, auth2));

        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(sender).sendMessage(argThat(containsString("Recently logged in players")));
        verify(sender).sendMessage(argThat(equalToIgnoringCase("- Hannah (08:09 AM, 11 Nov with IP 11.11.11.11)")));
        verify(sender).sendMessage(argThat(equalToIgnoringCase("- MATT (11:15 PM, 09 Nov with IP 22.11.22.33)")));
    }

    @Test
    void shouldHandlePlayerWithNullLastLogin() {
        // given
        PlayerAuth auth1 = PlayerAuth.builder()
            .name("xephren").realName("Xephren").lastIp("11.11.11.11")
            .lastLogin(null)
            .build();
        PlayerAuth auth2 = PlayerAuth.builder()
            .name("silvah777").realName("silvah777").lastIp("22.11.22.33")
            .lastLogin(1510269301000L) // 11/09/2017 @ 11:15pm
            .build();
        doReturn(ZoneId.of("UTC")).when(command).getZoneId();
        given(dataSource.getRecentlyLoggedInPlayers()).willReturn(Arrays.asList(auth1, auth2));

        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(sender).sendMessage(argThat(containsString("Recently logged in players")));
        verify(sender).sendMessage(argThat(equalToIgnoringCase("- Xephren (never with IP 11.11.11.11)")));
        verify(sender).sendMessage(argThat(equalToIgnoringCase("- silvah777 (11:15 PM, 09 Nov with IP 22.11.22.33)")));
    }
}
