package uk.org.whoami.authme.events;

import uk.org.whoami.authme.cache.auth.PlayerAuth;

/**
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
