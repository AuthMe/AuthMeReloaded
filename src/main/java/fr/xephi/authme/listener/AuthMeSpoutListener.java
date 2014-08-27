package fr.xephi.authme.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.getspout.spoutapi.event.spout.SpoutCraftEnableEvent;

import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.gui.screens.LoginScreen;
import fr.xephi.authme.settings.SpoutCfg;

public class AuthMeSpoutListener implements Listener {

    private DataSource data;

    public AuthMeSpoutListener(DataSource data) {
        this.data = data;
    }

    @EventHandler
    public void onSpoutCraftEnable(final SpoutCraftEnableEvent event) {
        if (SpoutCfg.getInstance().getBoolean("LoginScreen.enabled")) {
            if (data.isAuthAvailable(event.getPlayer().getName()) && !PlayerCache.getInstance().isAuthenticated(event.getPlayer().getName())) {
                event.getPlayer().getMainScreen().attachPopupScreen(new LoginScreen(event.getPlayer()));
            }
        }
    }
}
