package fr.xephi.authme.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is called when a player logout through AuthMe.
 *
 * @author Xephi59
 * @version $Revision: 1.0 $
 */
public class LogoutEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private Player player;

    /**
     * Constructor for LogoutEvent.
     *
     * @param player Player
     */
    public LogoutEvent(Player player) {
        this.player = player;
    }

    /**
     * Method getHandlerList.
     *
     * @return HandlerList
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * Method getPlayer.
     *
     * @return Player
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Method setPlayer.
     *
     * @param player Player
     */
    public void setPlayer(Player player) {
        this.player = player;
    }

    /**
     * Method getHandlers.
     *
     * @return HandlerList
     */
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

}
