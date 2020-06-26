package fr.xephi.authme.task.purge;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PurgeSettings;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

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
        verifyNoInteractions(bukkitService, dataSource);
    }

    @Test
    public void shouldNotRunAutoPurgeForInvalidInterval() {
        // given
        given(settings.getProperty(PurgeSettings.USE_AUTO_PURGE)).willReturn(true);
        given(settings.getProperty(PurgeSettings.DAYS_BEFORE_REMOVE_PLAYER)).willReturn(0);

        // when
        purgeService.runAutoPurge();

        // then
        verifyNoInteractions(bukkitService, dataSource);
    }

    @Test
    public void shouldRunAutoPurge() {
        // given
        given(settings.getProperty(PurgeSettings.USE_AUTO_PURGE)).willReturn(true);
        given(settings.getProperty(PurgeSettings.DAYS_BEFORE_REMOVE_PLAYER)).willReturn(60);
        Set<String> playerNames = newHashSet("alpha", "bravo", "charlie", "delta");
        given(dataSource.getRecordsToPurge(anyLong())).willReturn(playerNames);

        // when
        purgeService.runAutoPurge();

        // then
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(dataSource).getRecordsToPurge(captor.capture());
        assertCorrectPurgeTimestamp(captor.getValue(), 60);
        assertThat(Boolean.TRUE, equalTo(
            ReflectionTestUtils.getFieldValue(PurgeService.class, purgeService, "isPurging")));
        verifyScheduledPurgeTask(null, playerNames);
    }

    @Test
    public void shouldRecognizeNoPlayersToPurge() {
        // given
        final long delay = 123012301L;
        given(dataSource.getRecordsToPurge(delay)).willReturn(Collections.emptySet());
        CommandSender sender = mock(CommandSender.class);

        // when
        purgeService.runPurge(sender, delay);

        // then
        verify(dataSource).getRecordsToPurge(delay);
        verify(dataSource, never()).purgeRecords(anyCollection());
        verify(sender).sendMessage("No players to purge");
        verifyNoInteractions(bukkitService, permissionsManager);
    }

    @Test
    public void shouldRunPurge() {
        // given
        final long delay = 1809714L;
        Set<String> playerNames = newHashSet("charlie", "delta", "echo", "foxtrot");
        given(dataSource.getRecordsToPurge(delay)).willReturn(playerNames);
        Player sender = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        given(sender.getUniqueId()).willReturn(uuid);

        // when
        purgeService.runPurge(sender, delay);

        // then
        verify(dataSource).getRecordsToPurge(delay);
        verifyScheduledPurgeTask(uuid, playerNames);
    }

    @Test
    public void shouldNotRunPurgeIfProcessIsAlreadyRunning() {
        // given
        purgeService.setPurging(true);
        CommandSender sender = mock(CommandSender.class);
        OfflinePlayer[] offlinePlayers = new OfflinePlayer[]{mock(OfflinePlayer.class), mock(OfflinePlayer.class)};

        // when
        purgeService.purgePlayers(sender, newHashSet("test", "names"), offlinePlayers);

        // then
        verify(sender).sendMessage(argThat(containsString("Purge is already in progress")));
        verifyNoInteractions(bukkitService, dataSource, permissionsManager);
    }

    @Test
    public void shouldExecutePurgeActions() {
        // given
        List<String> names = Arrays.asList("alpha", "bravo", "foxtrot");
        List<OfflinePlayer> offlinePlayers = Arrays.asList(
            mock(OfflinePlayer.class), mock(OfflinePlayer.class), mock(OfflinePlayer.class));

        // when
        purgeService.executePurge(offlinePlayers, names);

        // then
        verify(executor).executePurge(offlinePlayers, names);
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
        verify(bukkitService).runTaskTimerAsynchronously(captor.capture(), eq(0L), eq(1L));
        PurgeTask task = captor.getValue();

        Object senderInTask = ReflectionTestUtils.getFieldValue(PurgeTask.class, task, "sender");
        Set<String> namesInTask = ReflectionTestUtils.getFieldValue(PurgeTask.class, task, "toPurge");
        assertThat(senderInTask, equalTo(senderUuid));
        assertThat(namesInTask, containsInAnyOrder(names.toArray()));
    }
}
