package fr.xephi.authme.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 *
 * @author Xephi59
 * @version $Revision: 1.0 $
 */
public class CustomEvent extends Event implements Cancellable {

    private boolean isCancelled;
    private static final HandlerList handlers = new HandlerList();

    public CustomEvent() {
        super(false);
    }

    /**
     * Constructor for CustomEvent.
     * @param b boolean
     */
    public CustomEvent(boolean b) {
        super(b);
    }

    /**
     * Method getHandlers.
    
     * @return HandlerList */
    public HandlerList getHandlers() {
        return handlers;
    }

    /**
     * Method getHandlerList.
    
     * @return HandlerList */
    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * Method isCancelled.
    
    
     * @return boolean * @see org.bukkit.event.Cancellable#isCancelled() */
    public boolean isCancelled() {
        return this.isCancelled;
    }

    /**
     * Method setCancelled.
     * @param cancelled boolean
    
     * @see org.bukkit.event.Cancellable#setCancelled(boolean) */
    public void setCancelled(boolean cancelled) {
        this.isCancelled = cancelled;
    }

}
