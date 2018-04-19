package fr.xephi.authme.task.purge;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link PurgeTask}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PurgeTaskTest {

    private static final PermissionNode BYPASS_NODE = PlayerStatePermission.BYPASS_PURGE;

    private Map<OfflinePlayer, Boolean> playerBypassAssignments = new HashMap<>();

    @Mock
    private PermissionsManager permissionsManager;

    @Mock
    private PurgeService purgeService;

    @Captor
    private ArgumentCaptor<Collection<OfflinePlayer>> playerCaptor;

    @Captor
    private ArgumentCaptor<Collection<String>> namesCaptor;

    @BeforeClass
    public static void initLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldRunTask() {
        // given
        Set<String> names =
            newHashSet("alpha", "bravo", "charlie", "delta", "echo", "foxtrot", "golf", "hotel", "india");
        // alpha and echo have bypass permission
        // Foxtrot and india are not present as OfflinePlayer
        // Additionally, BOGUS and 123456 are not present in the names list
        OfflinePlayer[] players = asArray(
            mockOfflinePlayer("Alpha", true),  mockOfflinePlayer("BOGUS", false),  mockOfflinePlayer("charlie", false),
            mockOfflinePlayer("Delta", false), mockOfflinePlayer("BRAVO", false),  mockOfflinePlayer("Echo", true),
            mockOfflinePlayer("Golf", false),  mockOfflinePlayer("123456", false), mockOfflinePlayer("HOTEL", false));
        reset(purgeService, permissionsManager);
        setPermissionsBehavior();
        PurgeTask task = new PurgeTask(purgeService, permissionsManager, null, names, players);

        // when (1 - first run, 5 players per run)
        task.run();

        // then (1)
        // In the first run, Alpha to BRAVO (see players list above) went through. One of those players is not present
        // in the names list, so expect the permission manager to have been called four times
        verify(permissionsManager, times(4)).hasPermissionOffline(any(OfflinePlayer.class), eq(BYPASS_NODE));
        // Alpha has the bypass permission, so we expect charlie, Delta and BRAVO to be purged
        assertRanPurgeWithPlayers(players[2], players[3], players[4]);

        // when (2)
        reset(purgeService, permissionsManager);
        setPermissionsBehavior();
        task.run();

        // then (2)
        // Echo, Golf, HOTEL
        verify(permissionsManager, times(3)).hasPermissionOffline(any(OfflinePlayer.class), eq(BYPASS_NODE));
        assertRanPurgeWithPlayers(players[6], players[8]);

        // given (3)
        // Third round: no more OfflinePlayer objects, but some names remain
        reset(purgeService, permissionsManager);
        given(permissionsManager.hasPermissionOffline("india", BYPASS_NODE)).willReturn(true);

        // when (3)
        task.run();

        // then (3)
        // We no longer have any OfflinePlayers, so lookup of permissions was done with the names
        verify(permissionsManager, times(2)).hasPermissionOffline(anyString(), eq(BYPASS_NODE));
        verify(permissionsManager, never()).hasPermissionOffline(any(OfflinePlayer.class), any(PermissionNode.class));
        assertRanPurgeWithNames("foxtrot");
    }

    /**
     * #1008: OfflinePlayer#getName may return null.
     */
    @Test
    public void shouldHandleOfflinePlayerWithNullName() {
        // given
        Set<String> names = newHashSet("name1", "name2");
        OfflinePlayer[] players = asArray(
            mockOfflinePlayer(null, false),  mockOfflinePlayer("charlie", false),  mockOfflinePlayer("name1", false));
        reset(purgeService, permissionsManager);
        setPermissionsBehavior();

        PurgeTask task = new PurgeTask(purgeService, permissionsManager, null, names, players);

        // when
        task.run();

        // then
        assertRanPurgeWithPlayers(players[2]);
    }

    @Test
    public void shouldStopTaskAndInformSenderUponCompletion() {
        // given
        Set<String> names = newHashSet("name1", "name2");
        Player sender = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        given(sender.getUniqueId()).willReturn(uuid);
        PurgeTask task = new PurgeTask(purgeService, permissionsManager, sender, names, new OfflinePlayer[0]);

        BukkitTask bukkitTask = mock(BukkitTask.class);
        given(bukkitTask.getTaskId()).willReturn(10049);
        ReflectionTestUtils.setField(BukkitRunnable.class, task, "task", bukkitTask);

        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        given(server.getScheduler()).willReturn(scheduler);
        ReflectionTestUtils.setField(Bukkit.class, null, "server", server);
        given(server.getPlayer(uuid)).willReturn(sender);

        task.run(); // Run for the first time -> results in empty names list

        // when
        task.run();

        // then
        verify(scheduler).cancelTask(task.getTaskId());
        verify(sender).sendMessage(argThat(containsString("Database has been purged successfully")));
    }

    @Test
    public void shouldStopTaskAndInformConsoleUser() {
        // given
        Set<String> names = newHashSet("name1", "name2");
        PurgeTask task = new PurgeTask(purgeService, permissionsManager, null, names, new OfflinePlayer[0]);

        BukkitTask bukkitTask = mock(BukkitTask.class);
        given(bukkitTask.getTaskId()).willReturn(10049);
        ReflectionTestUtils.setField(BukkitRunnable.class, task, "task", bukkitTask);

        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        given(server.getScheduler()).willReturn(scheduler);
        ReflectionTestUtils.setField(Bukkit.class, null, "server", server);
        ConsoleCommandSender consoleSender = mock(ConsoleCommandSender.class);
        given(server.getConsoleSender()).willReturn(consoleSender);

        task.run(); // Run for the first time -> results in empty names list

        // when
        task.run();

        // then
        verify(scheduler).cancelTask(task.getTaskId());
        verify(consoleSender).sendMessage(argThat(containsString("Database has been purged successfully")));
    }


    private OfflinePlayer mockOfflinePlayer(String name, boolean hasBypassPermission) {
        OfflinePlayer player = mock(OfflinePlayer.class);
        given(player.getName()).willReturn(name);
        playerBypassAssignments.put(player, hasBypassPermission);
        return player;
    }

    private OfflinePlayer[] asArray(OfflinePlayer... players) {
        return players;
    }

    private void setPermissionsBehavior() {
        given(permissionsManager.hasPermissionOffline(any(OfflinePlayer.class), eq(BYPASS_NODE)))
            .willAnswer((Answer<Boolean>) invocationOnMock -> {
                OfflinePlayer player = invocationOnMock.getArgument(0);
                Boolean hasPermission = playerBypassAssignments.get(player);
                if (hasPermission == null) {
                    throw new IllegalStateException("Unexpected check of '" + BYPASS_NODE
                        + "' with player = " + player);
                }
                return hasPermission;
            });
        given(permissionsManager.loadUserData(any(OfflinePlayer.class))).willReturn(true);
    }

    private void assertRanPurgeWithPlayers(OfflinePlayer... players) {
        List<String> names = new ArrayList<>(players.length);
        for (OfflinePlayer player : players) {
            names.add(player.getName());
        }
        verify(purgeService).executePurge(playerCaptor.capture(), namesCaptor.capture());
        assertThat(namesCaptor.getValue(), containsInAnyOrder(names.toArray()));
        assertThat(playerCaptor.getValue(), containsInAnyOrder(players));
    }

    private void assertRanPurgeWithNames(String... names) {
        verify(purgeService).executePurge(playerCaptor.capture(), namesCaptor.capture());
        assertThat(namesCaptor.getValue(), containsInAnyOrder(names));
        assertThat(playerCaptor.getValue(), empty());
    }

}
