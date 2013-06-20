package uk.org.whoami.authme.listener;

/**
 * @Author Hoezef
 */

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.getspout.spoutapi.event.spout.SpoutCraftEnableEvent;


import uk.org.whoami.authme.cache.auth.PlayerCache;
import uk.org.whoami.authme.datasource.DataSource;
import uk.org.whoami.authme.gui.screens.LoginScreen;
import uk.org.whoami.authme.settings.SpoutCfg;

public class AuthMeSpoutListener implements Listener {
	private DataSource data;

    public AuthMeSpoutListener(DataSource data) {
        this.data = data; 
    }

	@EventHandler
	public void onSpoutCraftEnable(final SpoutCraftEnableEvent event) {
		if(SpoutCfg.getInstance().getBoolean("LoginScreen.enabled")) {
			if (data.isAuthAvailable(event.getPlayer().getName().toLowerCase()) && !PlayerCache.getInstance().isAuthenticated(event.getPlayer().getName().toLowerCase()) ) {
				event.getPlayer().getMainScreen().attachPopupScreen(new LoginScreen(event.getPlayer()));
			}
		}
	}
}
