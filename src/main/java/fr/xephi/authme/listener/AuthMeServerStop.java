package fr.xephi.authme.listener;

import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.settings.Settings;

public class AuthMeServerStop extends Thread {

	private AuthMe plugin;

	public AuthMeServerStop(AuthMe plugin) {
		this.plugin = plugin;
	}

    public void run() {
    	// TODO: add a MessageKey
    	if (Settings.kickPlayersBeforeStopping) {
    		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
    		{
				@Override
				public void run() {
		            for (Player p : plugin.getServer().getOnlinePlayers()) {
		                p.kickPlayer("Server is restarting");
		            }
				}
    		});
        }
    }
}
