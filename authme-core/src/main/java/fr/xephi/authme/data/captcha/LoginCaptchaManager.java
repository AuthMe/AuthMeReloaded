package fr.xephi.authme.data.captcha;

import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.expiring.TimedCounter;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Manager for the handling of captchas after too many failed login attempts.
 */
public class LoginCaptchaManager implements CaptchaManager, SettingsDependent, HasCleanup {

    private final TimedCounter<String> playerCounts;
    private final CaptchaCodeStorage captchaCodeStorage;

    private boolean isEnabled;
    private int threshold;

    @Inject
    LoginCaptchaManager(Settings settings) {
        // Note: Proper values are set in reload()
        this.captchaCodeStorage = new CaptchaCodeStorage(30, 4);
        this.playerCounts = new TimedCounter<>(9, TimeUnit.MINUTES);
        reload(settings);
    }

    /**
     * Increases the failure count for the given player.
     *
     * @param name the player's name
     */
    public void increaseLoginFailureCount(String name) {
        if (isEnabled) {
            String playerLower = name.toLowerCase(Locale.ROOT);
            playerCounts.increment(playerLower);
        }
    }

    @Override
    public boolean isCaptchaRequired(String playerName) {
        return isEnabled && playerCounts.get(playerName.toLowerCase(Locale.ROOT)) >= threshold;
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
            playerCounts.remove(nameLower);
        }
        return isCodeCorrect;
    }

    /**
     * Resets the login count of the given player to 0.
     *
     * @param name the player's name
     */
    public void resetLoginFailureCount(String name) {
        if (isEnabled) {
            playerCounts.remove(name.toLowerCase(Locale.ROOT));
        }
    }

    @Override
    public void reload(Settings settings) {
        int expirationInMinutes = settings.getProperty(SecuritySettings.CAPTCHA_COUNT_MINUTES_BEFORE_RESET);
        captchaCodeStorage.setExpirationInMinutes(expirationInMinutes);
        int captchaLength = settings.getProperty(SecuritySettings.CAPTCHA_LENGTH);
        captchaCodeStorage.setCaptchaLength(captchaLength);

        int countTimeout = settings.getProperty(SecuritySettings.CAPTCHA_COUNT_MINUTES_BEFORE_RESET);
        playerCounts.setExpiration(countTimeout, TimeUnit.MINUTES);

        isEnabled = settings.getProperty(SecuritySettings.ENABLE_LOGIN_FAILURE_CAPTCHA);
        threshold = settings.getProperty(SecuritySettings.MAX_LOGIN_TRIES_BEFORE_CAPTCHA);
    }

    @Override
    public void performCleanup() {
        playerCounts.removeExpiredEntries();
        captchaCodeStorage.removeExpiredEntries();
    }
}
