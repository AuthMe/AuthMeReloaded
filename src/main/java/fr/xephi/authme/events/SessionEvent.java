package fr.xephi.authme.events;

import fr.xephi.authme.cache.auth.PlayerAuth;

/**
 *
 * This event is call when a player logging in through a timed session
 *
 * @author Xephi59
 */
public class SessionEvent extends CustomEvent {

    private PlayerAuth player;
    private boolean isLogin;

    public SessionEvent(PlayerAuth auth, boolean isLogin) {
        this.player = auth;
        this.isLogin = isLogin;
    }

    public PlayerAuth getPlayerAuth() {
        return this.player;
    }

    public void setPlayer(PlayerAuth player) {
        this.player = player;
    }

    public boolean isLogin() {
        return isLogin;
    }

}
