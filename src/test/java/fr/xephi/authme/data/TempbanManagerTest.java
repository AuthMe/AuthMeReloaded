package fr.xephi.authme.data;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.TempbanManager.TimedCounter;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

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
        Settings settings = mockSettings(3, 60);
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
        Settings settings = mockSettings(3, 60);
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
        Settings settings = mockSettings(1, 5);
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
        Settings settings = mockSettings(1, 5);
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
        Settings settings = mockSettings(0, 0);
        given(settings.getProperty(SecuritySettings.TEMPBAN_ON_MAX_LOGINS)).willReturn(false);
        Player player = mock(Player.class);
        TempbanManager manager = new TempbanManager(bukkitService, messages, settings);

        // when
        manager.tempbanPlayer(player);

        // then
        verifyZeroInteractions(player, bukkitService);
    }

    @Test
    public void shouldBanPlayerIp() {
        // given
        Player player = mock(Player.class);
        String ip = "123.45.67.89";
        TestHelper.mockPlayerIp(player, ip);
        String banReason = "IP ban too many logins";
        given(messages.retrieveSingle(MessageKey.TEMPBAN_MAX_LOGINS)).willReturn(banReason);
        Settings settings = mockSettings(2, 100);
        TempbanManager manager = new TempbanManager(bukkitService, messages, settings);

        // when
        manager.tempbanPlayer(player);
        TestHelper.runSyncDelayedTask(bukkitService);

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
    public void shouldResetCountAfterBan() {
        // given
        Player player = mock(Player.class);
        String ip = "22.44.66.88";
        TestHelper.mockPlayerIp(player, ip);
        String banReason = "kick msg";
        given(messages.retrieveSingle(MessageKey.TEMPBAN_MAX_LOGINS)).willReturn(banReason);
        Settings settings = mockSettings(10, 60);
        TempbanManager manager = new TempbanManager(bukkitService, messages, settings);
        manager.increaseCount(ip, "user");
        manager.increaseCount(ip, "name2");
        manager.increaseCount(ip, "user");

        // when
        manager.tempbanPlayer(player);
        TestHelper.runSyncDelayedTask(bukkitService);

        // then
        verify(player).kickPlayer(banReason);
        assertHasNoEntries(manager, ip);
    }

    @Test
    public void shouldPerformCleanup() {
        // given
        // `expirationPoint` is the approximate timestamp until which entries should be considered, so subtracting
        // from it will create expired entries, and adding a reasonably large number makes it still valid
        final long expirationPoint = System.currentTimeMillis() - TEST_EXPIRATION_THRESHOLD;
        // 2 current entries with total 6 failed tries
        Map<String, TimedCounter> map1 = new HashMap<>();
        map1.put("name", newTimedCounter(4, expirationPoint + 20_000));
        map1.put("other", newTimedCounter(2, expirationPoint + 40_000));
        // 0 current entries
        Map<String, TimedCounter> map2 = new HashMap<>();
        map2.put("someone", newTimedCounter(10, expirationPoint - 5_000));
        map2.put("somebody", newTimedCounter(10, expirationPoint - 8_000));
        // 1 current entry with total 4 failed tries
        Map<String, TimedCounter> map3 = new HashMap<>();
        map3.put("some", newTimedCounter(5, expirationPoint - 12_000));
        map3.put("test", newTimedCounter(4, expirationPoint + 8_000));
        map3.put("values", newTimedCounter(2, expirationPoint - 80_000));

        String[] addresses = {"123.45.67.89", "127.0.0.1", "192.168.0.1"};
        Map<String, Map<String, TimedCounter>> counterMap = new HashMap<>();
        counterMap.put(addresses[0], map1);
        counterMap.put(addresses[1], map2);
        counterMap.put(addresses[2], map3);

        TempbanManager manager = new TempbanManager(bukkitService, messages, mockSettings(5, 250));
        ReflectionTestUtils.setField(TempbanManager.class, manager, "ipLoginFailureCounts", counterMap);

        // when
        manager.performCleanup();

        // then
        assertThat(counterMap.get(addresses[0]), aMapWithSize(2));
        assertHasCount(manager, addresses[0], "name", 4);
        assertHasCount(manager, addresses[0], "other", 2);
        assertThat(counterMap.get(addresses[1]), anEmptyMap());
        assertThat(counterMap.get(addresses[2]), aMapWithSize(1));
        assertHasCount(manager, addresses[2], "test", 4);
    }

    private static Settings mockSettings(int maxTries, int tempbanLength) {
        Settings settings = mock(Settings.class);
        given(settings.getProperty(SecuritySettings.TEMPBAN_ON_MAX_LOGINS)).willReturn(true);
        given(settings.getProperty(SecuritySettings.MAX_LOGIN_TEMPBAN)).willReturn(maxTries);
        given(settings.getProperty(SecuritySettings.TEMPBAN_LENGTH)).willReturn(tempbanLength);
        given(settings.getProperty(SecuritySettings.TEMPBAN_MINUTES_BEFORE_RESET))
            .willReturn((int) TEST_EXPIRATION_THRESHOLD / 60_000);
        return settings;
    }

    private static void assertHasNoEntries(TempbanManager manager, String address) {
        Map<String, Map<String, TimedCounter>> playerCounts = ReflectionTestUtils
            .getFieldValue(TempbanManager.class, manager, "ipLoginFailureCounts");
        Map<String, TimedCounter> map = playerCounts.get(address);
        assertThat(map == null || map.isEmpty(), equalTo(true));
    }

    private static void assertHasCount(TempbanManager manager, String address, String name, int count) {
        Map<String, Map<String, TimedCounter>> playerCounts = ReflectionTestUtils
            .getFieldValue(TempbanManager.class, manager, "ipLoginFailureCounts");
        assertThat(playerCounts.get(address).get(name).getCount(TEST_EXPIRATION_THRESHOLD), equalTo(count));
    }

    private static TimedCounter newTimedCounter(int count, long timestamp) {
        TimedCounter counter = new TimedCounter(count);
        ReflectionTestUtils.setField(TimedCounter.class, counter, "lastIncrementTimestamp", timestamp);
        return counter;
    }
}
