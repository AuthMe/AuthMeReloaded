package fr.xephi.authme.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 *
 * This event is called when a player login or register through AuthMe. The
 * boolean 'isLogin' will be false if, and only if, login/register failed.
 *
 * @author Xephi59
 * @version $Revision: 1.0 $
 */
public class LoginEvent extends Event {

    private Player player;
    private boolean isLogin;
    private static final HandlerList handlers = new HandlerList();

    /**
     * Constructor for LoginEvent.
     * @param player Player
     * @param isLogin boolean
     */
    public LoginEvent(Player player, boolean isLogin) {
        this.player = player;
        this.isLogin = isLogin;
    }

    /**
     * Method getPlayer.
    
     * @return Player */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Method setPlayer.
     * @param player Player
     */
    public void setPlayer(Player player) {
        this.player = player;
    }

    /**
     * Method setLogin.
     * @param isLogin boolean
     */
    @Deprecated
    public void setLogin(boolean isLogin) {
        this.isLogin = isLogin;
    }

    /**
     * Method isLogin.
    
     * @return boolean */
    public boolean isLogin() {
        return isLogin;
    }

    /**
     * Method getHandlers.
    
     * @return HandlerList */
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /**
     * Method getHandlerList.
    
     * @return HandlerList */
    public static HandlerList getHandlerList() {
        return handlers;
    }

}
