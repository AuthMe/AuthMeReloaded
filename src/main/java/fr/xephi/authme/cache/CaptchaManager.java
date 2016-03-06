package fr.xephi.authme.cache;

import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.SecuritySettings;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for the handling of captchas.
 */
public class CaptchaManager {

    private final int threshold;
    private final int captchaLength;
    private final ConcurrentHashMap<String, Integer> playerCounts;
    private final ConcurrentHashMap<String, String> captchaCodes;

    public CaptchaManager(NewSetting settings) {
        this.playerCounts = new ConcurrentHashMap<>();
        this.captchaCodes = new ConcurrentHashMap<>();
        this.threshold = settings.getProperty(SecuritySettings.MAX_LOGIN_TRIES_BEFORE_CAPTCHA);
        this.captchaLength = settings.getProperty(SecuritySettings.CAPTCHA_LENGTH);
    }

    public void increaseCount(String player) {
        String playerLower = player.toLowerCase();
        Integer currentCount = playerCounts.get(playerLower);
        if (currentCount == null) {
            playerCounts.put(playerLower, 1);
        } else {
            playerCounts.put(playerLower, currentCount + 1);
        }
    }

    /**
     * Return whether the given player is required to solve a captcha.
     *
     * @param player The player to verify
     * @return True if the player has to solve a captcha, false otherwise
     */
    public boolean isCaptchaRequired(String player) {
        Integer count = playerCounts.get(player.toLowerCase());
        return count != null && count >= threshold;
    }

    /**
     * Return the captcha code for the player. Creates one if none present, so call only after
     * checking with {@link #isCaptchaRequired}.
     *
     * @param player The player
     * @return The code required for the player
     */
    public String getCaptchaCode(String player) {
        String code = captchaCodes.get(player.toLowerCase());
        if (code == null) {
            code = RandomString.generate(captchaLength);
            captchaCodes.put(player.toLowerCase(), code);
        }
        return code;
    }

    /**
     * Return whether the supplied code is correct for the given player.
     *
     * @param player The player to check
     * @param code The supplied code
     * @return True if the code matches or if no captcha is required for the player, false otherwise
     */
    public boolean checkCode(String player, String code) {
        String savedCode = captchaCodes.get(player.toLowerCase());
        if (savedCode == null) {
            return true;
        } else if (savedCode.equalsIgnoreCase(code)) {
            captchaCodes.remove(player.toLowerCase());
            return true;
        }
        return false;
    }

}
