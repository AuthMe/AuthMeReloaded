package fr.xephi.authme.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is called when a player logs out through AuthMe, i.e. only when the player
 * has executed the {@code /logout} command. This event is not fired if a player simply
 * leaves the server.
 */
public class LogoutEvent extends CustomEvent {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;

    /**
     * Constructor.
     *
     * @param player The player
     */
    public LogoutEvent(Player player) {
        this.player = player;
    }

    /**
     * Return the player who logged out.
     *
     * @return The player
     */
    public Player getPlayer() {
        return this.player;
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
