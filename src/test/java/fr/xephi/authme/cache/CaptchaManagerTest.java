package fr.xephi.authme.cache;

import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
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
        NewSetting settings = mockSettings(3, 4);
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
        NewSetting settings = mockSettings(1, 4);
        CaptchaManager manager = new CaptchaManager(settings);
        String captchaCode = manager.getCaptchaCode(player);

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


    private static NewSetting mockSettings(int maxTries, int captchaLength) {
        NewSetting settings = mock(NewSetting.class);
        given(settings.getProperty(SecuritySettings.MAX_LOGIN_TRIES_BEFORE_CAPTCHA)).willReturn(maxTries);
        given(settings.getProperty(SecuritySettings.CAPTCHA_LENGTH)).willReturn(captchaLength);
        return settings;
    }
}
