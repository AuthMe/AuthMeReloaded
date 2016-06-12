package fr.xephi.authme.cache;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link TempbanManager}.
 */
public class TempbanManagerTest {

    @Test
    public void shouldAddCounts() {
        // given
        NewSetting settings = mockSettings(3, 60);
        TempbanManager manager = new TempbanManager(settings);
        String player = "Tester";

        // when
        for (int i = 0; i < 2; ++i) {
            manager.increaseCount(player);
        }

        // then
        assertThat(manager.shouldTempban(player), equalTo(false));
        manager.increaseCount(player);
        assertThat(manager.shouldTempban(player), equalTo(true));
        assertThat(manager.shouldTempban("otherPlayer"), equalTo(false));
    }

    @Test
    public void shouldIncreaseAndResetCount() {
        // given
        String player = "plaYah";
        NewSetting settings = mockSettings(3, 60);
        TempbanManager manager = new TempbanManager(settings);

        // when
        manager.increaseCount(player);
        manager.increaseCount(player);
        manager.increaseCount(player);

        // then
        assertThat(manager.shouldTempban(player), equalTo(true));
        assertHasCount(manager, player, 3);

        // when 2
        manager.resetCount(player);

        // then 2
        assertThat(manager.shouldTempban(player), equalTo(false));
        assertHasCount(manager, player, null);
    }

    @Test
    public void shouldNotIncreaseCountForDisabledTempban() {
        // given
        String player = "playah";
        NewSetting settings = mockSettings(1, 5);
        given(settings.getProperty(SecuritySettings.TEMPBAN_ON_MAX_LOGINS)).willReturn(false);
        TempbanManager manager = new TempbanManager(settings);

        // when
        manager.increaseCount(player);

        // then
        assertThat(manager.shouldTempban(player), equalTo(false));
        assertHasCount(manager, player, null);
    }

    @Test
    public void shouldNotCheckCountIfTempbanIsDisabled() {
        // given
        String player = "playah";
        NewSetting settings = mockSettings(1, 5);
        TempbanManager manager = new TempbanManager(settings);
        given(settings.getProperty(SecuritySettings.TEMPBAN_ON_MAX_LOGINS)).willReturn(false);

        // when
        manager.increaseCount(player);
        // assumptions
        assertThat(manager.shouldTempban(player), equalTo(true));
        assertHasCount(manager, player, 1);
        // end assumptions
        manager.loadSettings(settings);
        boolean result = manager.shouldTempban(player);

        // then
        assertThat(result, equalTo(false));
    }

    private static NewSetting mockSettings(int maxTries, int tempbanLength) {
        NewSetting settings = mock(NewSetting.class);
        given(settings.getProperty(SecuritySettings.TEMPBAN_ON_MAX_LOGINS)).willReturn(true);
        given(settings.getProperty(SecuritySettings.MAX_LOGIN_TEMPBAN)).willReturn(maxTries);
        given(settings.getProperty(SecuritySettings.TEMPBAN_LENGTH)).willReturn(tempbanLength);
        return settings;
    }

    private static void assertHasCount(TempbanManager manager, String player, Integer count) {
        @SuppressWarnings("unchecked")
        Map<String, Integer> playerCounts = (Map<String, Integer>) ReflectionTestUtils
            .getFieldValue(TempbanManager.class, manager, "playerCounts");
        assertThat(playerCounts.get(player.toLowerCase()), equalTo(count));
    }
}
