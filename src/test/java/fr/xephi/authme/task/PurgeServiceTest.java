package fr.xephi.authme.task;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.hooks.PluginHooks;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.runner.BeforeInjecting;
import fr.xephi.authme.runner.InjectDelayed;
import fr.xephi.authme.runner.DelayedInjectionRunner;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.PurgeSettings;
import fr.xephi.authme.util.BukkitService;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Calendar;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link PurgeService}.
 */
@RunWith(DelayedInjectionRunner.class)
public class PurgeServiceTest {

    @InjectDelayed
    private PurgeService purgeService;

    @Mock
    private BukkitService bukkitService;
    @Mock
    private DataSource dataSource;
    @Mock
    private NewSetting settings;
    @Mock
    private PermissionsManager permissionsManager;
    @Mock
    private PluginHooks pluginHooks;
    @Mock
    private Server server;

    @BeforeClass
    public static void initLogger() {
        TestHelper.setupLogger();
    }

    @BeforeInjecting
    public void initSettingDefaults() {
        given(settings.getProperty(PurgeSettings.DAYS_BEFORE_REMOVE_PLAYER)).willReturn(60);
    }

    @Test
    public void shouldNotRunAutoPurge() {
        // given
        given(settings.getProperty(PurgeSettings.USE_AUTO_PURGE)).willReturn(false);

        // when
        purgeService.runAutoPurge();

        // then
        verifyZeroInteractions(bukkitService, dataSource);
    }

    @Test
    public void shouldNotRunAutoPurgeForInvalidInterval() {
        // given
        given(settings.getProperty(PurgeSettings.USE_AUTO_PURGE)).willReturn(true);
        given(settings.getProperty(PurgeSettings.DAYS_BEFORE_REMOVE_PLAYER)).willReturn(0);
        purgeService.reload();

        // when
        purgeService.runAutoPurge();

        // then
        verifyZeroInteractions(bukkitService, dataSource);
    }

    @Test
    public void shouldRunAutoPurge() {
        // given
        given(settings.getProperty(PurgeSettings.USE_AUTO_PURGE)).willReturn(true);
        given(settings.getProperty(PurgeSettings.DAYS_BEFORE_REMOVE_PLAYER)).willReturn(60);
        String[] playerNames = {"alpha", "bravo", "charlie", "delta"};
        given(dataSource.getRecordsToPurge(anyLong())).willReturn(newHashSet(playerNames));
        mockReturnedOfflinePlayers();
        mockHasBypassPurgePermission("bravo", "delta");

        // when
        purgeService.runAutoPurge();

        // then
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(dataSource).getRecordsToPurge(captor.capture());
        assertCorrectPurgeTimestamp(captor.getValue(), 60);
        verify(dataSource).purgeRecords(newHashSet("alpha", "charlie"));
        assertThat(purgeService.isPurging(), equalTo(true));
        verifyScheduledPurgeTask(null, "alpha", "charlie");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldRecognizeNoPlayersToPurge() {
        // given
        long delay = 123012301L;
        given(dataSource.getRecordsToPurge(delay)).willReturn(Collections.<String>emptySet());
        CommandSender sender = mock(CommandSender.class);

        // when
        purgeService.runPurge(sender, delay);

        // then
        verify(dataSource).getRecordsToPurge(delay);
        verify(dataSource, never()).purgeRecords(anySet());
        verify(sender).sendMessage("No players to purge");
        verifyZeroInteractions(bukkitService, permissionsManager);
    }

    @Test
    public void shouldRunPurge() {
        // given
        long delay = 1809714L;
        given(dataSource.getRecordsToPurge(delay)).willReturn(newHashSet("charlie", "delta", "echo", "foxtrot"));
        mockReturnedOfflinePlayers();
        mockHasBypassPurgePermission("echo");
        Player sender = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        given(sender.getUniqueId()).willReturn(uuid);

        // when
        purgeService.runPurge(sender, delay);

        // then
        verify(dataSource).getRecordsToPurge(delay);
        verify(dataSource).purgeRecords(newHashSet("charlie", "delta", "foxtrot"));
        verify(sender).sendMessage(argThat(containsString("Deleted 3 user accounts")));
        verifyScheduledPurgeTask(uuid, "charlie", "delta", "foxtrot");
    }

    @Test
    public void shouldRunPurgeIfProcessIsAlreadyRunning() {
        // given
        purgeService.setPurging(true);
        CommandSender sender = mock(CommandSender.class);
        OfflinePlayer[] players = mockReturnedOfflinePlayers();

        // when
        purgeService.purgePlayers(sender, newHashSet("test", "names"), players);

        // then
        verify(sender).sendMessage(argThat(containsString("Purge is already in progress")));
        verifyZeroInteractions(bukkitService, dataSource, permissionsManager);
    }

    /**
     * Returns mock OfflinePlayer objects with names corresponding to A - G of the NATO phonetic alphabet,
     * in various casing.
     *
     * @return list of offline players BukkitService is mocked to return
     */
    private OfflinePlayer[] mockReturnedOfflinePlayers() {
        String[] names = { "alfa", "Bravo", "charLIE", "delta", "ECHO", "Foxtrot", "golf" };
        OfflinePlayer[] players = new OfflinePlayer[names.length];
        for (int i = 0; i < names.length; ++i) {
            OfflinePlayer player = mock(OfflinePlayer.class);
            given(player.getName()).willReturn(names[i]);
            players[i] = player;
        }
        given(bukkitService.getOfflinePlayers()).willReturn(players);
        return players;
    }

    /**
     * Mocks the permission manager to say that the given names have the bypass purge permission.
     *
     * @param names the names
     */
    private void mockHasBypassPurgePermission(String... names) {
        for (String name : names) {
            given(permissionsManager.hasPermissionOffline(
                argThat(equalToIgnoringCase(name)), eq(PlayerStatePermission.BYPASS_PURGE))).willReturn(true);
        }
    }

    private void assertCorrectPurgeTimestamp(long timestamp, int configuredDays) {
        final long toleranceMillis = 100L;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -configuredDays);
        long expectedTimestamp = cal.getTimeInMillis();

        assertThat("Timestamp is equal to now minus " + configuredDays + " days (within tolerance)",
            Math.abs(timestamp - expectedTimestamp), not(greaterThan(toleranceMillis)));
    }

    @SuppressWarnings("unchecked")
    private void verifyScheduledPurgeTask(UUID uuid, String... names) {
        ArgumentCaptor<PurgeTask> captor = ArgumentCaptor.forClass(PurgeTask.class);
        verify(bukkitService).runTaskAsynchronously(captor.capture());
        PurgeTask task = captor.getValue();

        Object senderInTask = ReflectionTestUtils.getFieldValue(PurgeTask.class, task, "sender");
        Set<String> namesInTask = (Set<String>) ReflectionTestUtils.getFieldValue(PurgeTask.class, task, "toPurge");
        assertThat(senderInTask, Matchers.<Object>equalTo(uuid));
        assertThat(namesInTask, containsInAnyOrder(names));
    }
}
