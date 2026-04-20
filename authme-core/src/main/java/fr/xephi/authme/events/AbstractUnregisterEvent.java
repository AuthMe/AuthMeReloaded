package fr.xephi.authme.events;

import org.bukkit.entity.Player;

/**
 * Event fired when a player has been unregistered.
 */
public abstract class AbstractUnregisterEvent extends CustomEvent {

    private final Player player;

    /**
     * Constructor for a player that has unregistered himself.
     *
     * @param player the player
     * @param isAsync if the event is called asynchronously
     */
    public AbstractUnregisterEvent(Player player, boolean isAsync) {
        super(isAsync);
        this.player = player;
    }

    /**
     * Returns the player that has been unregistered.
     * <p>
     * This may be {@code null}! Please refer to the implementations of this class for details.
     *
     * @return the unregistered player, or null
     */
    public Player getPlayer() {
        return player;
    }
}
