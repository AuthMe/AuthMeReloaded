package fr.xephi.authme.process.register.executors;

import org.bukkit.entity.Player;

/**
 * Parent of all registration parameters.
 */
public abstract class RegistrationParameters {

    private final Player player;

    /**
     * Constructor.
     *
     * @param player the player to perform the registration for
     */
    public RegistrationParameters(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public String getPlayerName() {
        return player.getName();
    }
}
