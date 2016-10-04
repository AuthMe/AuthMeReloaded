package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.task.purge.PurgeService;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link PurgeBannedPlayersCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PurgeBannedPlayersCommandTest {

    @InjectMocks
    private PurgeBannedPlayersCommand command;

    @Mock
    private PurgeService purgeService;

    @Mock
    private BukkitService bukkitService;

    @Test
    public void shouldForwardRequestToService() {
        // given
        String[] names = {"bannedPlayer", "other_banned", "evilplayer", "Someone"};
        OfflinePlayer[] players = offlinePlayersWithNames(names);
        given(bukkitService.getBannedPlayers()).willReturn(newHashSet(players));
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.<String>emptyList());

        // then
        verify(bukkitService).getBannedPlayers();
        verify(purgeService).purgePlayers(eq(sender), eq(asLowerCaseSet(names)),
            argThat(arrayContainingInAnyOrder(players)));
    }

    private static OfflinePlayer[] offlinePlayersWithNames(String... names) {
        OfflinePlayer[] players = new OfflinePlayer[names.length];
        for (int i = 0; i < names.length; ++i) {
            OfflinePlayer player = mock(OfflinePlayer.class);
            given(player.getName()).willReturn(names[i]);
            players[i] = player;
        }
        return players;
    }

    private static Set<String> asLowerCaseSet(String... items) {
        Set<String> result = new HashSet<>(items.length);
        for (String item : items) {
            result.add(item.toLowerCase());
        }
        return result;
    }
}
