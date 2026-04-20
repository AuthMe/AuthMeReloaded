package fr.xephi.authme.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is called when a player uses the login command,
 * it's fired even when a user does a /login with invalid password.
 * {@link #setCanLogin(boolean) event.setCanLogin(false)} prevents the player from logging in.
 */
public class AuthMeAsyncPreLoginEvent extends CustomEvent {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private boolean canLogin = true;

    /**
     * Constructor.
     *
     * @param player The player
     * @param isAsync True if the event is async, false otherwise
     */
    public AuthMeAsyncPreLoginEvent(Player player, boolean isAsync) {
        super(isAsync);
        this.player = player;
    }

    /**
     * Return the player concerned by this event.
     *
     * @return The player who executed a valid {@code /login} command
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Return whether the player is allowed to log in.
     *
     * @return True if the player can log in, false otherwise
     */
    public boolean canLogin() {
        return canLogin;
    }

    /**
     * Define whether or not the player may log in.
     *
     * @param canLogin True to allow the player to log in; false to prevent him
     */
    public void setCanLogin(boolean canLogin) {
        this.canLogin = canLogin;
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
