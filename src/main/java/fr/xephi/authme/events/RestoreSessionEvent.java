package fr.xephi.authme.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Event fired before a session is restored.
 */
public class RestoreSessionEvent extends CustomEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private boolean isCancelled;

    public RestoreSessionEvent(Player player, boolean isAsync) {
        super(isAsync);
        this.player = player;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    /**
     * @return the player for which the session will be enabled
     */
    public Player getPlayer() {
        return player;
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
