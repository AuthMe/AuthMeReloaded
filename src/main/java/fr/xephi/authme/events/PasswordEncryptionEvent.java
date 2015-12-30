package fr.xephi.authme.events;

import fr.xephi.authme.security.crypts.EncryptionMethod;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is called when we need to compare or hash password and allows
 * third-party listeners to change the encryption method. This is typically
 * done with the {@link fr.xephi.authme.security.HashAlgorithm#CUSTOM} setting.
 *
 * @author Xephi59
 */
public class PasswordEncryptionEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private EncryptionMethod method;
    private String playerName;

    public PasswordEncryptionEvent(EncryptionMethod method, String playerName) {
        super(false);
        this.method = method;
        this.playerName = playerName;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public EncryptionMethod getMethod() {
        return method;
    }

    public void setMethod(EncryptionMethod method) {
        this.method = method;
    }

    public String getPlayerName() {
        return playerName;
    }

}
