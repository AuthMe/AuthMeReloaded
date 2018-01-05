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
     * Generates a code for the player and returns it.
     *
     * @param name the name of the player to generate a code for
     * @return the generated code
     */
    String generateCode(String name);

    /**
     * Checks the given code against the existing one. This method may perform additional state changes
     * on success or failure, such as modifying some counter or setting a player as verified.
     *
     * @param player the player to check
     * @param code the supplied code
     * @return true if the code matches, false otherwise
     */
    boolean checkCode(Player player, String code);

}
