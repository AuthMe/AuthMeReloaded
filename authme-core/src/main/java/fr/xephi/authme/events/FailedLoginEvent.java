package fr.xephi.authme.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a player enters a wrong password.
 */
public class FailedLoginEvent extends CustomEvent {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;

    /**
     * Constructor.
     *
     * @param player The player
     * @param isAsync if the event is called asynchronously
     */
    public FailedLoginEvent(Player player, boolean isAsync) {
        super(isAsync);
        this.player = player;
    }

    /**
     * @return The player entering a wrong password
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Return the list of handlers, equivalent to {@link #getHandlers()} and required by {@link Event}.
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
