package fr.xephi.authme.data;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link CaptchaManager}.
 */
public class CaptchaManagerTest {

    @Test
    public void shouldAddCounts() {
        // given
        Settings settings = mockSettings(3, 4);
        CaptchaManager manager = new CaptchaManager(settings);
        String player = "tester";

        // when
        for (int i = 0; i < 2; ++i) {
            manager.increaseCount(player);
        }

        // then
        assertThat(manager.isCaptchaRequired(player), equalTo(false));
        manager.increaseCount(player);
        assertThat(manager.isCaptchaRequired(player.toUpperCase()), equalTo(true));
        assertThat(manager.isCaptchaRequired("otherPlayer"), equalTo(false));
    }

    @Test
    public void shouldCreateAndCheckCaptcha() {
        // given
        String player = "Miner";
        Settings settings = mockSettings(1, 4);
        CaptchaManager manager = new CaptchaManager(settings);
        String captchaCode = manager.getCaptchaCodeOrGenerateNew(player);

        // when
        boolean badResult = manager.checkCode(player, "wrong_code");
        boolean goodResult = manager.checkCode(player, captchaCode);

        // then
        assertThat(captchaCode.length(), equalTo(4));
        assertThat(badResult, equalTo(false));
        assertThat(goodResult, equalTo(true));
        // Supplying correct code should clear the entry, and any code should be valid if no entry is present
        assertThat(manager.checkCode(player, "bogus"), equalTo(true));
    }

    /**
     * Tests {@link CaptchaManager#getCaptchaCode} and {@link CaptchaManager#getCaptchaCodeOrGenerateNew}.
     * The former method should never change the code (and so return {@code null} for no code) while the latter should
     * generate a new code if no code is yet present. If a code is saved, it should never generate a new one.
     */
    @Test
    public void shouldHaveSameCodeAfterGeneration() {
        // given
        String player = "Tester";
        Settings settings = mockSettings(1, 5);
        CaptchaManager manager = new CaptchaManager(settings);

        // when
        String code1 = manager.getCaptchaCode(player);
        String code2 = manager.getCaptchaCodeOrGenerateNew(player);
        String code3 = manager.getCaptchaCode(player);
        String code4 = manager.getCaptchaCodeOrGenerateNew(player);
        String code5 = manager.getCaptchaCode(player);

        // then
        assertThat(code1, nullValue());
        assertThat(code2.length(), equalTo(5));
        assertThat(code3, equalTo(code2));
        assertThat(code4, equalTo(code2));
        assertThat(code5, equalTo(code2));
    }

    @Test
    public void shouldIncreaseAndResetCount() {
        // given
        String player = "plaYer";
        Settings settings = mockSettings(2, 3);
        CaptchaManager manager = new CaptchaManager(settings);

        // when
        manager.increaseCount(player);
        manager.increaseCount(player);

        // then
        assertThat(manager.isCaptchaRequired(player), equalTo(true));
        assertHasCount(manager, player, 2);

        // when 2
        manager.resetCounts(player);

        // then 2
        assertThat(manager.isCaptchaRequired(player), equalTo(false));
        assertHasCount(manager, player, null);
    }

    @Test
    public void shouldNotIncreaseCountForDisabledCaptcha() {
        // given
        String player = "someone_";
        Settings settings = mockSettings(1, 3);
        given(settings.getProperty(SecuritySettings.USE_CAPTCHA)).willReturn(false);
        CaptchaManager manager = new CaptchaManager(settings);

        // when
        manager.increaseCount(player);

        // then
        assertThat(manager.isCaptchaRequired(player), equalTo(false));
        assertHasCount(manager, player, null);
    }

    @Test
    public void shouldNotCheckCountIfCaptchaIsDisabled() {
        // given
        String player = "Robert001";
        Settings settings = mockSettings(1, 5);
        CaptchaManager manager = new CaptchaManager(settings);
        given(settings.getProperty(SecuritySettings.USE_CAPTCHA)).willReturn(false);

        // when
        manager.increaseCount(player);
        // assumptions
        assertThat(manager.isCaptchaRequired(player), equalTo(true));
        assertHasCount(manager, player, 1);
        // end assumptions
        manager.reload(settings);
        boolean result = manager.isCaptchaRequired(player);

        // then
        assertThat(result, equalTo(false));
    }

    private static Settings mockSettings(int maxTries, int captchaLength) {
        Settings settings = mock(Settings.class);
        given(settings.getProperty(SecuritySettings.USE_CAPTCHA)).willReturn(true);
        given(settings.getProperty(SecuritySettings.MAX_LOGIN_TRIES_BEFORE_CAPTCHA)).willReturn(maxTries);
        given(settings.getProperty(SecuritySettings.CAPTCHA_LENGTH)).willReturn(captchaLength);
        return settings;
    }

    private static void assertHasCount(CaptchaManager manager, String player, Integer count) {
        Map<String, Integer> playerCounts = ReflectionTestUtils
            .getFieldValue(CaptchaManager.class, manager, "playerCounts");
        assertThat(playerCounts.get(player.toLowerCase()), equalTo(count));
    }
}
