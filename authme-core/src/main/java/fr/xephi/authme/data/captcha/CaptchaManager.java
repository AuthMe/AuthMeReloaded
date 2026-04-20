package fr.xephi.authme.data.captcha;

import org.bukkit.entity.Player;

/**
 * Manages captcha codes.
 */
public interface CaptchaManager {

    /**
     * Returns whether the given player is required to solve a captcha.
     *
     * @param name the name of the player to verify
     * @return true if the player has to solve a captcha, false otherwise
     */
    boolean isCaptchaRequired(String name);

    /**
     * Returns the stored captcha for the player or generates and saves a new one.
     *
     * @param name the player's name
     * @return the code the player is required to enter
     */
    String getCaptchaCodeOrGenerateNew(String name);

    /**
     * Checks the given code against the existing one. This method is not reentrant, i.e. it performs additional
     * state changes on success or failure, such as modifying some counter or setting a player as verified.
     * <p>
     * On success, the code associated with the player is cleared; on failure, a new code is generated.
     *
     * @param player the player to check
     * @param code the supplied code
     * @return true if the code matches, false otherwise
     */
    boolean checkCode(Player player, String code);

}
