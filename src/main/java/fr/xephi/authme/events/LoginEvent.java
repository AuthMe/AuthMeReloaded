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
 */
public class LoginEvent extends Event {

    private Player player;
    private boolean isLogin;
    private static final HandlerList handlers = new HandlerList();

    public LoginEvent(Player player, boolean isLogin) {
        this.player = player;
        this.isLogin = isLogin;
    }

    public Player getPlayer() {
        return this.player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    @Deprecated
    public void setLogin(boolean isLogin) {
        this.isLogin = isLogin;
    }

    public boolean isLogin() {
        return isLogin;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
