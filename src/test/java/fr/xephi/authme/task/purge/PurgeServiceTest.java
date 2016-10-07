package fr.xephi.authme.task.purge;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PurgeSettings;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link PurgeService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PurgeServiceTest {

    @InjectMocks
    private PurgeService purgeService;

    @Mock
    private BukkitService bukkitService;
    @Mock
    private DataSource dataSource;
    @Mock
    private Settings settings;
    @Mock
    private PermissionsManager permissionsManager;
    @Mock
    private PurgeExecutor executor;

    @BeforeClass
    public static void initLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldNotRunAutoPurge() {
        // given
        given(settings.getProperty(PurgeSettings.USE_AUTO_PURGE)).willReturn(false);
        given(settings.getProperty(PurgeSettings.DAYS_BEFORE_REMOVE_PLAYER)).willReturn(60);

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
        Set<String> playerNames = newHashSet("alpha", "bravo", "charlie", "delta");
        given(dataSource.getRecordsToPurge(anyLong(), eq(false))).willReturn(playerNames);
        mockReturnedOfflinePlayers();

        // when
        purgeService.runAutoPurge();

        // then
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(dataSource).getRecordsToPurge(captor.capture(), eq(false));
        assertCorrectPurgeTimestamp(captor.getValue(), 60);
        assertThat(Boolean.TRUE, equalTo(
            ReflectionTestUtils.getFieldValue(PurgeService.class, purgeService, "isPurging")));
        verifyScheduledPurgeTask(null, playerNames);
    }

    @Test
    public void shouldRecognizeNoPlayersToPurge() {
        // given
        final long delay = 123012301L;
        final boolean includeLastLoginZeroEntries = true;
        given(dataSource.getRecordsToPurge(delay, includeLastLoginZeroEntries)).willReturn(Collections.<String>emptySet());
        CommandSender sender = mock(CommandSender.class);

        // when
        purgeService.runPurge(sender, delay, includeLastLoginZeroEntries);

        // then
        verify(dataSource).getRecordsToPurge(delay, includeLastLoginZeroEntries);
        verify(dataSource, never()).purgeRecords(anyCollectionOf(String.class));
        verify(sender).sendMessage("No players to purge");
        verifyZeroInteractions(bukkitService, permissionsManager);
    }

    @Test
    public void shouldRunPurge() {
        // given
        final long delay = 1809714L;
        final boolean includeLastLoginZeroEntries = false;
        Set<String> playerNames = newHashSet("charlie", "delta", "echo", "foxtrot");
        given(dataSource.getRecordsToPurge(delay, includeLastLoginZeroEntries)).willReturn(playerNames);
        mockReturnedOfflinePlayers();
        Player sender = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        given(sender.getUniqueId()).willReturn(uuid);

        // when
        purgeService.runPurge(sender, delay, includeLastLoginZeroEntries);

        // then
        verify(dataSource).getRecordsToPurge(delay, includeLastLoginZeroEntries);
        verifyScheduledPurgeTask(uuid, playerNames);
    }

    @Test
    public void shouldNotRunPurgeIfProcessIsAlreadyRunning() {
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

    @Test
    public void shouldExecutePurgeActions() {
        // given
        List<OfflinePlayer> players = Arrays.asList(mockReturnedOfflinePlayers());
        List<String> names = Arrays.asList("alpha", "bravo", "foxtrot");

        // when
        purgeService.executePurge(players, names);

        // then
        verify(executor).executePurge(players, names);
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

    private void assertCorrectPurgeTimestamp(long timestamp, int configuredDays) {
        final long toleranceMillis = 100L;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -configuredDays);
        long expectedTimestamp = cal.getTimeInMillis();

        assertThat("Timestamp is equal to now minus " + configuredDays + " days (within tolerance)",
            Math.abs(timestamp - expectedTimestamp), not(greaterThan(toleranceMillis)));
    }

    private void verifyScheduledPurgeTask(UUID senderUuid, Set<String> names) {
        ArgumentCaptor<PurgeTask> captor = ArgumentCaptor.forClass(PurgeTask.class);
        verify(bukkitService).runTaskTimer(captor.capture(), eq(0L), eq(1L));
        PurgeTask task = captor.getValue();

        Object senderInTask = ReflectionTestUtils.getFieldValue(PurgeTask.class, task, "sender");
        Set<String> namesInTask = ReflectionTestUtils.getFieldValue(PurgeTask.class, task, "toPurge");
        assertThat(senderInTask, equalTo(senderUuid));
        assertThat(namesInTask, containsInAnyOrder(names.toArray()));
    }
}
