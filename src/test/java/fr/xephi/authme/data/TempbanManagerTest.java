package fr.xephi.authme.data;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.expiring.TimedCounter;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static fr.xephi.authme.service.BukkitServiceTestHelper.setBukkitServiceToScheduleSyncDelayedTask;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link TempbanManager}.
 */
@RunWith(MockitoJUnitRunner.class)
public class TempbanManagerTest {

    private static final long DATE_TOLERANCE_MILLISECONDS = 200L;
    private static final long TEST_EXPIRATION_THRESHOLD = 120_000L;

    @Mock
    private BukkitService bukkitService;

    @Mock
    private Messages messages;

    @Test
    public void shouldAddCounts() {
        // given
        Settings settings = mockSettings(3, 60, "");
        TempbanManager manager = new TempbanManager(bukkitService, messages, settings);
        String address = "192.168.1.1";

        // when
        manager.increaseCount(address, "Bob");
        manager.increaseCount(address, "Todd");

        // then
        assertThat(manager.shouldTempban(address), equalTo(false));
        assertHasCount(manager, address, "Bob", 1);
        assertHasCount(manager, address, "Todd", 1);
        manager.increaseCount(address, "Bob");
        assertThat(manager.shouldTempban(address), equalTo(true));
        assertThat(manager.shouldTempban("10.0.0.1"), equalTo(false));
    }

    @Test
    public void shouldIncreaseAndResetCount() {
        // given
        String address = "192.168.1.2";
        Settings settings = mockSettings(3, 60, "");
        TempbanManager manager = new TempbanManager(bukkitService, messages, settings);

        // when
        manager.increaseCount(address, "test");
        manager.increaseCount(address, "test");
        manager.increaseCount(address, "test");

        // then
        assertThat(manager.shouldTempban(address), equalTo(true));
        assertHasCount(manager, address, "test", 3);

        // when 2
        manager.resetCount(address, "test");

        // then 2
        assertThat(manager.shouldTempban(address), equalTo(false));
        assertHasNoEntries(manager, address);
    }

    @Test
    public void shouldNotIncreaseCountForDisabledTempban() {
        // given
        String address = "192.168.1.3";
        Settings settings = mockSettings(1, 5, "");
        given(settings.getProperty(SecuritySettings.TEMPBAN_ON_MAX_LOGINS)).willReturn(false);
        TempbanManager manager = new TempbanManager(bukkitService, messages, settings);

        // when
        manager.increaseCount(address, "username");

        // then
        assertThat(manager.shouldTempban(address), equalTo(false));
        assertHasNoEntries(manager, address);
    }

    @Test
    public void shouldNotCheckCountIfTempbanIsDisabled() {
        // given
        String address = "192.168.1.4";
        Settings settings = mockSettings(1, 5, "");
        TempbanManager manager = new TempbanManager(bukkitService, messages, settings);
        given(settings.getProperty(SecuritySettings.TEMPBAN_ON_MAX_LOGINS)).willReturn(false);

        // when
        manager.increaseCount(address, "username");
        // assumptions
        assertThat(manager.shouldTempban(address), equalTo(true));
        assertHasCount(manager, address, "username", 1);
        // end assumptions
        manager.reload(settings);
        boolean result = manager.shouldTempban(address);

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    public void shouldNotIssueBanIfDisabled() {
        // given
        Settings settings = mockSettings(0, 0, "");
        given(settings.getProperty(SecuritySettings.TEMPBAN_ON_MAX_LOGINS)).willReturn(false);
        Player player = mock(Player.class);
        TempbanManager manager = new TempbanManager(bukkitService, messages, settings);

        // when
        manager.tempbanPlayer(player);

        // then
        verifyNoInteractions(player, bukkitService);
    }

    @Test
    public void shouldBanPlayerIp() {
        // given
        Player player = mock(Player.class);
        String ip = "123.45.67.89";
        TestHelper.mockIpAddressToPlayer(player, ip);
        String banReason = "IP ban too many logins";
        given(messages.retrieveSingle(player, MessageKey.TEMPBAN_MAX_LOGINS)).willReturn(banReason);
        Settings settings = mockSettings(2, 100, "");
        TempbanManager manager = new TempbanManager(bukkitService, messages, settings);
        setBukkitServiceToScheduleSyncDelayedTask(bukkitService);

        // when
        manager.tempbanPlayer(player);

        // then
        verify(player).kickPlayer(banReason);
        ArgumentCaptor<Date> captor = ArgumentCaptor.forClass(Date.class);
        verify(bukkitService).banIp(eq(ip), eq(banReason), captor.capture(), eq("AuthMe"));

        // Compute the expected expiration date and check that the actual date is within the difference tolerance
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, 100);
        long expectedExpiration = cal.getTime().getTime();
        assertThat(Math.abs(captor.getValue().getTime() - expectedExpiration), lessThan(DATE_TOLERANCE_MILLISECONDS));
    }

    @Test
    public void shouldBanPlayerIpCustom() {
        // given
        Player player = mock(Player.class);
        given(player.getName()).willReturn("Bob");
        String ip = "143.45.77.89";
        TestHelper.mockIpAddressToPlayer(player, ip);
        String banCommand = "banip %ip% 15d IP ban too many logins";
        Settings settings = mockSettings(2, 100, banCommand);
        TempbanManager manager = new TempbanManager(bukkitService, messages, settings);
        setBukkitServiceToScheduleSyncDelayedTask(bukkitService);

        // when
        manager.tempbanPlayer(player);

        // then
        verify(bukkitService).dispatchConsoleCommand(banCommand.replace("%ip%", ip));
    }

    @Test
    public void shouldResetCountAfterBan() {
        // given
        Player player = mock(Player.class);
        String ip = "22.44.66.88";
        TestHelper.mockIpAddressToPlayer(player, ip);
        String banReason = "kick msg";
        given(messages.retrieveSingle(player, MessageKey.TEMPBAN_MAX_LOGINS)).willReturn(banReason);
        Settings settings = mockSettings(10, 60, "");
        TempbanManager manager = new TempbanManager(bukkitService, messages, settings);
        manager.increaseCount(ip, "user");
        manager.increaseCount(ip, "name2");
        manager.increaseCount(ip, "user");
        setBukkitServiceToScheduleSyncDelayedTask(bukkitService);

        // when
        manager.tempbanPlayer(player);

        // then
        verify(player).kickPlayer(banReason);
        assertHasNoEntries(manager, ip);
    }

    @Test
    public void shouldPerformCleanup() {
        // given
        Map<String, TimedCounter<String>> counts = new HashMap<>();
        TimedCounter<String> counter1 = mockCounter();
        given(counter1.isEmpty()).willReturn(true);
        counts.put("11.11.11.11", counter1);
        TimedCounter<String> counter2 = mockCounter();
        given(counter2.isEmpty()).willReturn(false);
        counts.put("33.33.33.33", counter2);

        TempbanManager manager = new TempbanManager(bukkitService, messages, mockSettings(3, 10, ""));
        ReflectionTestUtils.setField(TempbanManager.class, manager, "ipLoginFailureCounts", counts);

        // when
        manager.performCleanup();

        // then
        verify(counter1).removeExpiredEntries();
        verify(counter2).removeExpiredEntries();
        assertThat(counts.keySet(), contains("33.33.33.33"));
    }

    private static Settings mockSettings(int maxTries, int tempbanLength, String customCommand) {
        Settings settings = mock(Settings.class);
        given(settings.getProperty(SecuritySettings.TEMPBAN_ON_MAX_LOGINS)).willReturn(true);
        given(settings.getProperty(SecuritySettings.MAX_LOGIN_TEMPBAN)).willReturn(maxTries);
        given(settings.getProperty(SecuritySettings.TEMPBAN_LENGTH)).willReturn(tempbanLength);
        given(settings.getProperty(SecuritySettings.TEMPBAN_MINUTES_BEFORE_RESET))
            .willReturn((int) TEST_EXPIRATION_THRESHOLD / 60_000);
        given(settings.getProperty(SecuritySettings.TEMPBAN_CUSTOM_COMMAND)).willReturn(customCommand);
        return settings;
    }

    private static void assertHasNoEntries(TempbanManager manager, String address) {
        Map<String, TimedCounter<String>> playerCounts = ReflectionTestUtils
            .getFieldValue(TempbanManager.class, manager, "ipLoginFailureCounts");
        TimedCounter<String> counter = playerCounts.get(address);
        assertThat(counter == null || counter.isEmpty(), equalTo(true));
    }

    private static void assertHasCount(TempbanManager manager, String address, String name, int count) {
        Map<String, TimedCounter<String>> playerCounts = ReflectionTestUtils
            .getFieldValue(TempbanManager.class, manager, "ipLoginFailureCounts");
        assertThat(playerCounts.get(address).get(name), equalTo(count));
    }

    @SuppressWarnings("unchecked")
    private static <T> TimedCounter<T> mockCounter() {
        return mock(TimedCounter.class);
    }
}
