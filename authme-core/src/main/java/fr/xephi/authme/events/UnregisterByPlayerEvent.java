package fr.xephi.authme.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Event fired after a player has unregistered himself.
 */
public class UnregisterByPlayerEvent extends AbstractUnregisterEvent {

    private static final HandlerList handlers = new HandlerList();

    /**
     * Constructor.
     *
     * @param player the player (never null)
     * @param isAsync if the event is called asynchronously
     */
    public UnregisterByPlayerEvent(Player player, boolean isAsync) {
        super(player, isAsync);
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
