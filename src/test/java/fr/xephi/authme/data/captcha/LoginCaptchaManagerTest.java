package fr.xephi.authme.data.captcha;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.expiring.TimedCounter;
import org.bukkit.entity.Player;
import org.junit.Test;

import java.util.Locale;

import static fr.xephi.authme.AuthMeMatchers.stringWithLength;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link LoginCaptchaManager}.
 */
public class LoginCaptchaManagerTest {

    @Test
    public void shouldAddCounts() {
        // given
        Settings settings = mockSettings(3, 4);
        LoginCaptchaManager manager = new LoginCaptchaManager(settings);
        String player = "tester";

        // when
        for (int i = 0; i < 2; ++i) {
            manager.increaseLoginFailureCount(player);
        }

        // then
        assertThat(manager.isCaptchaRequired(player), equalTo(false));
        manager.increaseLoginFailureCount(player);
        assertThat(manager.isCaptchaRequired(player.toUpperCase(Locale.ROOT)), equalTo(true));
        assertThat(manager.isCaptchaRequired("otherPlayer"), equalTo(false));
    }

    @Test
    public void shouldCreateAndCheckCaptcha() {
        // given
        String name = "Miner";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        Settings settings = mockSettings(1, 4);
        LoginCaptchaManager manager = new LoginCaptchaManager(settings);
        String captchaCode = manager.getCaptchaCodeOrGenerateNew(name);

        // when
        boolean result = manager.checkCode(player, captchaCode);

        // then
        assertThat(captchaCode, stringWithLength(4));
        assertThat(result, equalTo(true));
        // Supplying correct code should clear the entry, and a code should be invalid if no entry is present
        assertThat(manager.checkCode(player, "bogus"), equalTo(false));
    }

    @Test
    public void shouldGenerateNewCodeOnFailure() {
        // given
        String name = "Tarheel";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        Settings settings = mockSettings(1, 9);
        LoginCaptchaManager manager = new LoginCaptchaManager(settings);
        String captchaCode = manager.getCaptchaCodeOrGenerateNew(name);

        // when
        boolean result = manager.checkCode(player, "wrongcode");

        // then
        assertThat(captchaCode, stringWithLength(9));
        assertThat(result, equalTo(false));
        assertThat(manager.getCaptchaCodeOrGenerateNew(name), not(equalTo(captchaCode)));
    }

    @Test
    public void shouldHaveSameCodeAfterGeneration() {
        // given
        String player = "Tester";
        Settings settings = mockSettings(1, 5);
        LoginCaptchaManager manager = new LoginCaptchaManager(settings);

        // when
        String code1 = manager.getCaptchaCodeOrGenerateNew(player);
        String code2 = manager.getCaptchaCodeOrGenerateNew(player);
        String code3 = manager.getCaptchaCodeOrGenerateNew(player);

        // then
        assertThat(code1.length(), equalTo(5));
        assertThat(code2, equalTo(code1));
        assertThat(code3, equalTo(code1));
    }

    @Test
    public void shouldIncreaseAndResetCount() {
        // given
        String player = "plaYer";
        Settings settings = mockSettings(2, 3);
        LoginCaptchaManager manager = new LoginCaptchaManager(settings);

        // when
        manager.increaseLoginFailureCount(player);
        manager.increaseLoginFailureCount(player);

        // then
        assertThat(manager.isCaptchaRequired(player), equalTo(true));
        assertHasCount(manager, player, 2);

        // when 2
        manager.resetLoginFailureCount(player);

        // then 2
        assertThat(manager.isCaptchaRequired(player), equalTo(false));
        assertHasCount(manager, player, 0);
    }

    @Test
    public void shouldNotIncreaseCountForDisabledCaptcha() {
        // given
        String player = "someone_";
        Settings settings = mockSettings(1, 3);
        given(settings.getProperty(SecuritySettings.ENABLE_LOGIN_FAILURE_CAPTCHA)).willReturn(false);
        LoginCaptchaManager manager = new LoginCaptchaManager(settings);

        // when
        manager.increaseLoginFailureCount(player);

        // then
        assertThat(manager.isCaptchaRequired(player), equalTo(false));
        assertHasCount(manager, player, 0);
    }

    @Test
    public void shouldNotCheckCountIfCaptchaIsDisabled() {
        // given
        String player = "Robert001";
        Settings settings = mockSettings(1, 5);
        LoginCaptchaManager manager = new LoginCaptchaManager(settings);
        given(settings.getProperty(SecuritySettings.ENABLE_LOGIN_FAILURE_CAPTCHA)).willReturn(false);

        // when
        manager.increaseLoginFailureCount(player);
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
        given(settings.getProperty(SecuritySettings.ENABLE_LOGIN_FAILURE_CAPTCHA)).willReturn(true);
        given(settings.getProperty(SecuritySettings.MAX_LOGIN_TRIES_BEFORE_CAPTCHA)).willReturn(maxTries);
        given(settings.getProperty(SecuritySettings.CAPTCHA_LENGTH)).willReturn(captchaLength);
        given(settings.getProperty(SecuritySettings.CAPTCHA_COUNT_MINUTES_BEFORE_RESET)).willReturn(30);
        return settings;
    }

    private static void assertHasCount(LoginCaptchaManager manager, String player, Integer count) {
        TimedCounter<String> playerCounts = ReflectionTestUtils
            .getFieldValue(LoginCaptchaManager.class, manager, "playerCounts");
        assertThat(playerCounts.get(player.toLowerCase(Locale.ROOT)), equalTo(count));
    }
}
