package fr.xephi.authme.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a player has successfully registered.
 */
public class RegisterEvent extends CustomEvent {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;

    /**
     * Constructor.
     *
     * @param player The player
     */
    public RegisterEvent(Player player) {
        this.player = player;
    }

    /**
     * Return the player that has successfully logged in or registered.
     *
     * @return The player
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
