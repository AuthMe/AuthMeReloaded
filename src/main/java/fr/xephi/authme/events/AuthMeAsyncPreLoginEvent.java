package fr.xephi.authme.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 *
 * This event is call when a player try to /login
 *
 * @author Xephi59
 * @version $Revision: 1.0 $
 */
public class AuthMeAsyncPreLoginEvent extends Event {

    private Player player;
    private boolean canLogin = true;
    private static final HandlerList handlers = new HandlerList();

    /**
     * Constructor for AuthMeAsyncPreLoginEvent.
     * @param player Player
     */
    public AuthMeAsyncPreLoginEvent(Player player) {
        super(true);
        this.player = player;
    }

    /**
     * Method getPlayer.
    
     * @return Player */
    public Player getPlayer() {
        return player;
    }

    /**
     * Method canLogin.
    
     * @return boolean */
    public boolean canLogin() {
        return canLogin;
    }

    /**
     * Method setCanLogin.
     * @param canLogin boolean
     */
    public void setCanLogin(boolean canLogin) {
        this.canLogin = canLogin;
    }

    /**
     * Method getHandlers.
    
     * @return HandlerList */
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

}
