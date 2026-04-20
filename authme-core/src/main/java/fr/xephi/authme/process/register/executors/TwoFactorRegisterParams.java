package fr.xephi.authme.process.register.executors;

import org.bukkit.entity.Player;

/**
 * Parameters for registration with two-factor authentication.
 */
public class TwoFactorRegisterParams extends AbstractPasswordRegisterParams {

    protected TwoFactorRegisterParams(Player player) {
        super(player);
    }

    /**
     * Creates a parameters object.
     *
     * @param player the player to register
     * @return params object with the given player
     */
    public static TwoFactorRegisterParams of(Player player) {
        return new TwoFactorRegisterParams(player);
    }
}
