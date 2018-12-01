package fr.xephi.authme.events;

import fr.xephi.authme.data.player.OnlineIdentifier;
import org.bukkit.entity.Player;

/**
 * Event fired when a player has been unregistered.
 */
public abstract class AbstractUnregisterEvent extends CustomEvent {

    private final OnlineIdentifier identifier;

    /**
     * Constructor for a player that has unregistered himself.
     *
     * @param identifier the player identifier
     * @param isAsync if the event is called asynchronously
     */
    public AbstractUnregisterEvent(OnlineIdentifier identifier, boolean isAsync) {
        super(isAsync);
        this.identifier = identifier;
    }

    /**
     * Returns the player that has been unregistered.
     * <p>
     * This may be {@code null}! Please refer to the implementations of this class for details.
     *
     * @return the unregistered player, or null
     */
    public Player getPlayer() {
        return identifier.getPlayer();
    }

    /**
     * Returns the identifier of the player that has been unregistered.
     * <p>
     * This may be {@code null}! Please refer to the implementations of this class for details.
     *
     * @return the unregistered player identifier, or null
     */
    public OnlineIdentifier getIdentifier() {
        return identifier;
    }
}
