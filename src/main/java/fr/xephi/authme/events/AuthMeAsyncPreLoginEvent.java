package fr.xephi.authme.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 *
 * This event is call when a player try to /login
 *
 * @author Xephi59
 */
public class AuthMeAsyncPreLoginEvent extends Event {

    private Player player;
    private boolean canLogin = true;
    private static final HandlerList handlers = new HandlerList();

    public AuthMeAsyncPreLoginEvent(Player player) {
        super(true);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean canLogin() {
        return canLogin;
    }

    public void setCanLogin(boolean canLogin) {
        this.canLogin = canLogin;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

}
