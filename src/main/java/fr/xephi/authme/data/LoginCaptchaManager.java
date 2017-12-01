package fr.xephi.authme.data;

import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.expiring.TimedCounter;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * Manager for the handling of captchas after too many failed login attempts.
 */
public class LoginCaptchaManager extends AbstractCaptchaManager {

    // Note: proper expiration is set in reload(), which is also called on initialization by the parent
    private final TimedCounter<String> playerCounts = new TimedCounter<>(0, TimeUnit.MINUTES);

    private boolean isEnabled;
    private int threshold;

    @Inject
    LoginCaptchaManager(Settings settings) {
        super(settings);
        reload(settings);
    }

    /**
     * Increases the failure count for the given player.
     *
     * @param name the player's name
     */
    public void increaseLoginFailureCount(String name) {
        if (isEnabled) {
            String playerLower = name.toLowerCase();
            playerCounts.increment(playerLower);
        }
    }

    @Override
    public boolean isCaptchaRequired(String playerName) {
        return isEnabled && playerCounts.get(playerName.toLowerCase()) >= threshold;
    }

    /**
     * Resets the login count of the given player to 0.
     *
     * @param name the player's name
     */
    public void resetLoginFailureCount(String name) {
        if (isEnabled) {
            playerCounts.remove(name.toLowerCase());
        }
    }

    @Override
    public void reload(Settings settings) {
        super.reload(settings);

        this.isEnabled = settings.getProperty(SecuritySettings.ENABLE_LOGIN_FAILURE_CAPTCHA);
        this.threshold = settings.getProperty(SecuritySettings.MAX_LOGIN_TRIES_BEFORE_CAPTCHA);
        long countTimeout = settings.getProperty(SecuritySettings.CAPTCHA_COUNT_MINUTES_BEFORE_RESET);
        playerCounts.setExpiration(countTimeout, TimeUnit.MINUTES);
    }

    @Override
    public void performCleanup() {
        super.performCleanup();
        playerCounts.removeExpiredEntries();
    }

    @Override
    protected void processSuccessfulCode(String nameLower) {
        playerCounts.remove(nameLower);
    }

    @Override
    protected int minutesBeforeCodeExpires(Settings settings) {
        return settings.getProperty(SecuritySettings.CAPTCHA_COUNT_MINUTES_BEFORE_RESET);
    }
}
