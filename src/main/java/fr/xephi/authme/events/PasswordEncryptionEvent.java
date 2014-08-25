package fr.xephi.authme.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import fr.xephi.authme.security.crypts.EncryptionMethod;

/**
 * <p>
 * This event is called when we need to compare or get an hash password, for set
 * a custom EncryptionMethod
 * </p>
 * 
 * @see fr.xephi.authme.security.crypts.EncryptionMethod
 * 
 * @author Xephi59
 */
public class PasswordEncryptionEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private EncryptionMethod method = null;
    private String playerName = "";

    public PasswordEncryptionEvent(EncryptionMethod method, String playerName) {
        this.method = method;
        this.playerName = playerName;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public void setMethod(EncryptionMethod method) {
        this.method = method;
    }

    public EncryptionMethod getMethod() {
        return method;
    }

    public String getPlayerName() {
        return playerName;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
