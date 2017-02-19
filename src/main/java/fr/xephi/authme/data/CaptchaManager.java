package fr.xephi.authme.data;

import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.RandomStringUtils;
import fr.xephi.authme.util.TimedCounter;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manager for the handling of captchas.
 */
public class CaptchaManager implements SettingsDependent, HasCleanup {

    private final TimedCounter<String> playerCounts;
    private final ConcurrentHashMap<String, String> captchaCodes;

    private boolean isEnabled;
    private int threshold;
    private int captchaLength;

    @Inject
    CaptchaManager(Settings settings) {
        this.captchaCodes = new ConcurrentHashMap<>();
        long countTimeout = settings.getProperty(SecuritySettings.CAPTCHA_COUNT_MINUTES_BEFORE_RESET);
        this.playerCounts = new TimedCounter<>(countTimeout, TimeUnit.MINUTES);
        reload(settings);
    }

    /**
     * Increases the failure count for the given player.
     *
     * @param name the player's name
     */
    public void increaseCount(String name) {
        if (isEnabled) {
            String playerLower = name.toLowerCase();
            playerCounts.increment(playerLower);
        }
    }

    /**
     * Returns whether the given player is required to solve a captcha.
     *
     * @param name the name of the player to verify
     * @return true if the player has to solve a captcha, false otherwise
     */
    public boolean isCaptchaRequired(String name) {
        return isEnabled && playerCounts.get(name.toLowerCase()) >= threshold;
    }

    /**
     * Returns the stored captcha for the player or generates and saves a new one.
     *
     * @param name the player's name
     * @return the code the player is required to enter
     */
    public String getCaptchaCodeOrGenerateNew(String name) {
        String code = captchaCodes.get(name.toLowerCase());
        return code == null ? generateCode(name) : code;
    }

    /**
     * Generates a code for the player and returns it.
     *
     * @param name the name of the player to generate a code for
     * @return the generated code
     */
    public String generateCode(String name) {
        String code = RandomStringUtils.generate(captchaLength);
        captchaCodes.put(name.toLowerCase(), code);
        return code;
    }

    /**
     * Checks the given code against the existing one and resets the player's auth failure count upon success.
     *
     * @param name the name of the player to check
     * @param code the supplied code
     * @return true if the code matches or if no captcha is required for the player, false otherwise
     */
    public boolean checkCode(String name, String code) {
        String savedCode = captchaCodes.get(name.toLowerCase());
        if (savedCode == null) {
            return true;
        } else if (savedCode.equalsIgnoreCase(code)) {
            captchaCodes.remove(name.toLowerCase());
            playerCounts.remove(name.toLowerCase());
            return true;
        }
        return false;
    }

    /**
     * Resets the login count of the given player to 0.
     *
     * @param name the player's name
     */
    public void resetCounts(String name) {
        if (isEnabled) {
            captchaCodes.remove(name.toLowerCase());
            playerCounts.remove(name.toLowerCase());
        }
    }

    @Override
    public void reload(Settings settings) {
        this.isEnabled = settings.getProperty(SecuritySettings.USE_CAPTCHA);
        this.threshold = settings.getProperty(SecuritySettings.MAX_LOGIN_TRIES_BEFORE_CAPTCHA);
        this.captchaLength = settings.getProperty(SecuritySettings.CAPTCHA_LENGTH);
        long countTimeout = settings.getProperty(SecuritySettings.CAPTCHA_COUNT_MINUTES_BEFORE_RESET);
        playerCounts.setExpiration(countTimeout, TimeUnit.MINUTES);
    }

    @Override
    public void performCleanup() {
        playerCounts.removeExpiredEntries();
    }

}
