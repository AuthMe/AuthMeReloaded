package fr.xephi.authme.events;

import fr.xephi.authme.data.player.OnlineIdentifier;
import org.bukkit.event.HandlerList;

/**
 * Event fired after a player has unregistered himself.
 */
public class UnregisterByPlayerEvent extends AbstractUnregisterEvent {

    private static final HandlerList handlers = new HandlerList();

    /**
     * Constructor.
     *
     * @param identifier the player identifier (never null)
     * @param isAsync if the event is called asynchronously
     */
    public UnregisterByPlayerEvent(OnlineIdentifier identifier, boolean isAsync) {
        super(identifier, isAsync);
    }

    /**
     * Return the list of handlers, equivalent to {@link #getHandlers()} and required by {@link org.bukkit.event.Event}.
     *
     * @return The list of handlers
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
