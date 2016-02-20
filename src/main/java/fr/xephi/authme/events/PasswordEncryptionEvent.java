package fr.xephi.authme.events;

import fr.xephi.authme.security.crypts.EncryptionMethod;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is called when we need to compare or hash a password for a player and allows
 * third-party listeners to change the encryption method. This is typically
 * done with the {@link fr.xephi.authme.security.HashAlgorithm#CUSTOM} setting.
 */
public class PasswordEncryptionEvent extends CustomEvent {

    private static final HandlerList handlers = new HandlerList();
    private EncryptionMethod method;
    private String playerName;

    /**
     * Constructor.
     *
     * @param method The method used to encrypt the password
     * @param playerName The name of the player
     */
    public PasswordEncryptionEvent(EncryptionMethod method, String playerName) {
        super(false);
        this.method = method;
        this.playerName = playerName;
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

    /**
     * Return the encryption method used to hash the password.
     *
     * @return The encryption method
     */
    public EncryptionMethod getMethod() {
        return method;
    }

    /**
     * Set the encryption method to hash the password with.
     *
     * @param method The encryption method to use
     */
    public void setMethod(EncryptionMethod method) {
        this.method = method;
    }

    /**
     * Return the name of the player the event has been fired for.
     *
     * @return The player name
     */
    public String getPlayerName() {
        return playerName;
    }

}
