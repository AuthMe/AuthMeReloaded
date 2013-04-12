package uk.org.whoami.authme.events;

import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
*
* @author Xephi59
*/
public class UncancellableEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private static Server s;

	public HandlerList getHandlers() {
		return handlers;
	}

    public static HandlerList getHandlerList() {
        return handlers;
    }

	public static Server getServer() {
		return s;
	}

}
