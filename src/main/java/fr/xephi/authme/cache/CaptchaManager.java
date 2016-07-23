package fr.xephi.authme.cache;

import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for the handling of captchas.
 */
public class CaptchaManager implements SettingsDependent {

    private final ConcurrentHashMap<String, Integer> playerCounts;
    private final ConcurrentHashMap<String, String> captchaCodes;

    private boolean isEnabled;
    private int threshold;
    private int captchaLength;

    @Inject
    CaptchaManager(Settings settings) {
        this.playerCounts = new ConcurrentHashMap<>();
        this.captchaCodes = new ConcurrentHashMap<>();
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
            Integer currentCount = playerCounts.get(playerLower);
            if (currentCount == null) {
                playerCounts.put(playerLower, 1);
            } else {
                playerCounts.put(playerLower, currentCount + 1);
            }
        }
    }

    /**
     * Returns whether the given player is required to solve a captcha.
     *
     * @param name the name of the player to verify
     * @return true if the player has to solve a captcha, false otherwise
     */
    public boolean isCaptchaRequired(String name) {
        if (isEnabled) {
            Integer count = playerCounts.get(name.toLowerCase());
            return count != null && count >= threshold;
        }
        return false;
    }

    /**
     * Returns the stored captcha code for the player.
     *
     * @param name the player's name
     * @return the code the player is required to enter, or null if none registered
     */
    public String getCaptchaCode(String name) {
        return captchaCodes.get(name.toLowerCase());
    }

    /**
     * Returns the stored captcha for the player or generates and saves a new one.
     *
     * @param name the player's name
     * @return the code the player is required to enter
     */
    public String getCaptchaCodeOrGenerateNew(String name) {
        String code = getCaptchaCode(name);
        return code == null ? generateCode(name) : code;
    }

    /**
     * Generates a code for the player and returns it.
     *
     * @param name the name of the player to generate a code for
     * @return the generated code
     */
    public String generateCode(String name) {
        String code = RandomString.generate(captchaLength);
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
    }

}
