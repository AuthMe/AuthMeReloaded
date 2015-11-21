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
 * @version $Revision: 1.0 $
 */
public class PasswordEncryptionEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private EncryptionMethod method = null;
    private String playerName = "";

    /**
     * Constructor for PasswordEncryptionEvent.
     * @param method EncryptionMethod
     * @param playerName String
     */
    public PasswordEncryptionEvent(EncryptionMethod method, String playerName) {
        super(false);
        this.method = method;
        this.playerName = playerName;
    }

    /**
     * Method getHandlers.
     * @return HandlerList
     */
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /**
     * Method setMethod.
     * @param method EncryptionMethod
     */
    public void setMethod(EncryptionMethod method) {
        this.method = method;
    }

    /**
     * Method getMethod.
     * @return EncryptionMethod
     */
    public EncryptionMethod getMethod() {
        return method;
    }

    /**
     * Method getPlayerName.
     * @return String
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Method getHandlerList.
     * @return HandlerList
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }

}
