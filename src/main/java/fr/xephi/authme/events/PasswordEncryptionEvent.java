package fr.xephi.authme.events;

import fr.xephi.authme.security.crypts.EncryptionMethod;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is called when we need to compare or get an hash password, for set
 * a custom EncryptionMethod
 *
 * @author Xephi59
 */
public class PasswordEncryptionEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private EncryptionMethod method = null;
    private String playerName = "";

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
