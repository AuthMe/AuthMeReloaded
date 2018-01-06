package fr.xephi.authme.data.captcha;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.expiring.ExpiringMap;
import org.bukkit.entity.Player;
import org.junit.Test;

import static fr.xephi.authme.AuthMeMatchers.stringWithLength;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link RegistrationCaptchaManager}.
 */
public class RegistrationCaptchaManagerTest {

    @Test
    public void shouldBeDisabled() {
        // given
        Settings settings = mock(Settings.class);
        // Return false first time, and true after that
        given(settings.getProperty(SecuritySettings.ENABLE_CAPTCHA_FOR_REGISTRATION))
            .willReturn(false).willReturn(true);
        given(settings.getProperty(SecuritySettings.CAPTCHA_LENGTH)).willReturn(12);

        // when
        RegistrationCaptchaManager captchaManager1 = new RegistrationCaptchaManager(settings);
        RegistrationCaptchaManager captchaManager2 = new RegistrationCaptchaManager(settings);

        // then
        assertThat(captchaManager1.isCaptchaRequired("bob"), equalTo(false));
        assertThat(captchaManager2.isCaptchaRequired("bob"), equalTo(true));
    }

    @Test
    public void shouldVerifyCodeSuccessfully() {
        // given
        Settings settings = mock(Settings.class);
        given(settings.getProperty(SecuritySettings.ENABLE_CAPTCHA_FOR_REGISTRATION)).willReturn(true);
        given(settings.getProperty(SecuritySettings.CAPTCHA_LENGTH)).willReturn(12);

        String captcha = "abc3";
        RegistrationCaptchaManager captchaManager = new RegistrationCaptchaManager(settings);
        getCodeMap(captchaManager).put("test", captcha);

        Player player = mock(Player.class);
        given(player.getName()).willReturn("TeSt");

        // when
        boolean isSuccessful = captchaManager.checkCode(player, captcha);

        // then
        assertThat(isSuccessful, equalTo(true));
        assertThat(getCodeMap(captchaManager).isEmpty(), equalTo(true));
        assertThat(captchaManager.isCaptchaRequired("test"), equalTo(false));
    }

    @Test
    public void shouldGenerateAndRetrieveCode() {
        // given
        Settings settings = mock(Settings.class);
        given(settings.getProperty(SecuritySettings.ENABLE_CAPTCHA_FOR_REGISTRATION)).willReturn(true);
        int captchaLength = 9;
        given(settings.getProperty(SecuritySettings.CAPTCHA_LENGTH)).willReturn(captchaLength);
        RegistrationCaptchaManager captchaManager = new RegistrationCaptchaManager(settings);

        // when
        String captcha1 = captchaManager.getCaptchaCodeOrGenerateNew("toast");
        String captcha2 = captchaManager.getCaptchaCodeOrGenerateNew("Toast");

        // then
        assertThat(captcha1, equalTo(captcha2));
        assertThat(captcha1, stringWithLength(captchaLength));

        // given (2)
        Player player = mock(Player.class);
        given(player.getName()).willReturn("toast");

        // when (2) / then (2)
        assertThat(captchaManager.checkCode(player, captcha1), equalTo(true));
    }

    @SuppressWarnings("unchecked")
    private static ExpiringMap<String, String> getCodeMap(RegistrationCaptchaManager captchaManager) {
        CaptchaCodeStorage captchaStorage = ReflectionTestUtils.getFieldValue(
            RegistrationCaptchaManager.class, captchaManager, "captchaCodeStorage");
        return ReflectionTestUtils.getFieldValue(CaptchaCodeStorage.class, captchaStorage, "captchaCodes");
    }
}
