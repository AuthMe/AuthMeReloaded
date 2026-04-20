package fr.xephi.authme.data.captcha;

import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.expiring.ExpiringSet;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Captcha manager for registration.
 */
public class RegistrationCaptchaManager implements CaptchaManager, SettingsDependent, HasCleanup {

    private static final int MINUTES_VALID_FOR_REGISTRATION = 30;

    private final ExpiringSet<String> verifiedNamesForRegistration;
    private final CaptchaCodeStorage captchaCodeStorage;
    private boolean isEnabled;

    @Inject
    RegistrationCaptchaManager(Settings settings) {
        // NOTE: proper captcha length is set in reload()
        this.captchaCodeStorage = new CaptchaCodeStorage(MINUTES_VALID_FOR_REGISTRATION, 4);
        this.verifiedNamesForRegistration = new ExpiringSet<>(MINUTES_VALID_FOR_REGISTRATION, TimeUnit.MINUTES);
        reload(settings);
    }

    @Override
    public boolean isCaptchaRequired(String name) {
        return isEnabled && !verifiedNamesForRegistration.contains(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public String getCaptchaCodeOrGenerateNew(String name) {
        return captchaCodeStorage.getCodeOrGenerateNew(name);
    }

    @Override
    public boolean checkCode(Player player, String code) {
        String nameLower = player.getName().toLowerCase(Locale.ROOT);
        boolean isCodeCorrect = captchaCodeStorage.checkCode(nameLower, code);
        if (isCodeCorrect) {
            verifiedNamesForRegistration.add(nameLower);
        }
        return isCodeCorrect;
    }

    @Override
    public void reload(Settings settings) {
        int captchaLength = settings.getProperty(SecuritySettings.CAPTCHA_LENGTH);
        captchaCodeStorage.setCaptchaLength(captchaLength);

        isEnabled = settings.getProperty(SecuritySettings.ENABLE_CAPTCHA_FOR_REGISTRATION);
    }

    @Override
    public void performCleanup() {
        verifiedNamesForRegistration.removeExpiredEntries();
        captchaCodeStorage.removeExpiredEntries();
    }
}
