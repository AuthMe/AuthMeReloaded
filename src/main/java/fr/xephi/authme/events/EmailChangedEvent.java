package fr.xephi.authme.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * This event is called when a player changes his email address.
 */
public class EmailChangedEvent extends CustomEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String oldEmail;
    private final String newEmail;
    private boolean isCancelled;

    public EmailChangedEvent(Player player, String oldEmail, String newEmail, boolean isAsync) {
        super(isAsync);
        this.player = player;
        this.oldEmail = oldEmail;
        this.newEmail = newEmail;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }
    
    public Player getPlayer() {
        return player;
    }

    public String getOldEmail() {
        return this.oldEmail;
    }

    public String getNewEmail() {
        return this.newEmail;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.isCancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
