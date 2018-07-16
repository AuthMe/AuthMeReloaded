package fr.xephi.authme.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import javax.annotation.Nullable;

/**
 * This event is called when a player adds or changes his email address.
 */
public class EmailChangedEvent extends CustomEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String oldEmail;
    private final String newEmail;
    private boolean isCancelled;

    /**
     * Constructor
     *
     * @param player The player that changed email
     * @param oldEmail Old email player had on file. Can be null when user adds an email
     * @param newEmail New email that player tries to set. In case of adding email, this will contain
     *                the email is trying to set.
     * @param isAsync should this event be called asynchronously?
     */
    public EmailChangedEvent(Player player, @Nullable String oldEmail, String newEmail, boolean isAsync) {
        super(isAsync);
        this.player = player;
        this.oldEmail = oldEmail;
        this.newEmail = newEmail;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    /**
     * Gets the player who changes the email
     *
     * @return The player who changed the email
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the old email in case user tries to change existing email.
     *
     * @return old email stored on file. Can be null when user never had an email and adds a new one.
     */
    public @Nullable String getOldEmail() {
        return this.oldEmail;
    }

    /**
     * Gets the new email.
     *
     * @return the email user is trying to set. If user adds email and never had one before,
     *        this is where such email can be found.
     */
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

    /**
     * Return the list of handlers, equivalent to {@link #getHandlers()} and required by {@link Event}.
     *
     * @return The list of handlers
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
