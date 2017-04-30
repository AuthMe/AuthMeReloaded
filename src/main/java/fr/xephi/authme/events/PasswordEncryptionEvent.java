package fr.xephi.authme.events;

import fr.xephi.authme.security.crypts.EncryptionMethod;
import org.bukkit.event.HandlerList;

/**
 * This event is called when we need to compare or hash a password for a player and allows
 * third-party listeners to change the encryption method. This is typically
 * done with the {@link fr.xephi.authme.security.HashAlgorithm#CUSTOM} setting.
 */
public class PasswordEncryptionEvent extends CustomEvent {

    private static final HandlerList handlers = new HandlerList();
    private EncryptionMethod method;

    /**
     * Constructor.
     *
     * @param method The method used to encrypt the password
     */
    public PasswordEncryptionEvent(EncryptionMethod method) {
        super(false);
        this.method = method;
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
}
