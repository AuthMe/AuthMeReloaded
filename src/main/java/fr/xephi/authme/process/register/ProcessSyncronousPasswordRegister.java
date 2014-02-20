package fr.xephi.authme.process.register;

import me.muizers.Notifications.Notification;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.Utils;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.RegisterTeleportEvent;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;

public class ProcessSyncronousPasswordRegister implements Runnable {

	protected Player player;
	protected String name;
	private AuthMe plugin;
	private Messages m = Messages.getInstance();
	public ProcessSyncronousPasswordRegister(Player player, AuthMe plugin) {
		this.player = player;
		this.name = player.getName().toLowerCase();
		this.plugin = plugin;
	}
	
    protected void forceCommands(Player player) {
    	for (String command : Settings.forceCommands) {
    		try {
    			player.performCommand(command.replace("%p", player.getName()));
    		} catch (Exception e) {}
    	}
    }
	@Override
	public void run() {
		LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
		if (limbo != null) {
		    player.setGameMode(limbo.getGameMode());      
		    if (Settings.isTeleportToSpawnEnabled) {
		    	World world = player.getWorld();
		    	Location loca = plugin.getSpawnLocation(name, world);
		        RegisterTeleportEvent tpEvent = new RegisterTeleportEvent(player, loca);
		        plugin.getServer().getPluginManager().callEvent(tpEvent);
		        if(!tpEvent.isCancelled()) {
		        	if (!tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).isLoaded()) {
		        		tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).load();
		        	}
		      	  	player.teleport(tpEvent.getTo());
		        }
		    }
		    plugin.getServer().getScheduler().cancelTask(limbo.getTimeoutTaskId());
		    plugin.getServer().getScheduler().cancelTask(limbo.getMessageTaskId());
		    LimboCache.getInstance().deleteLimboPlayer(name);
		}

		if(!Settings.getRegisteredGroup.isEmpty()){
		    Utils.getInstance().setGroup(player, Utils.groupType.REGISTERED);
		}
		m._(player, "registered");
		if (!Settings.getmailAccount.isEmpty())
			m._(player, "add_email");
		if (player.getGameMode() != GameMode.CREATIVE && !Settings.isMovementAllowed) {
		    player.setAllowFlight(false);
		    player.setFlying(false);
		}
		// The Loginevent now fires (as intended) after everything is processed
		Bukkit.getServer().getPluginManager().callEvent(new LoginEvent(player, true));
		player.saveData();
		
		// Register is finish and player is logged, display welcome message
		if(Settings.useWelcomeMessage)
		    if(Settings.broadcastWelcomeMessage) {
		        for (String s : Settings.welcomeMsg) {
		    		Bukkit.getServer().broadcastMessage(s);
		        }
		    } else {
		        for (String s : Settings.welcomeMsg) {
		        	player.sendMessage(plugin.replaceAllInfos(s, player));
		        }
		    }

		// Register is now finish , we can force all commands
		forceCommands(player);
		if (!Settings.noConsoleSpam)
		ConsoleLogger.info(player.getName() + " registered "+player.getAddress().getAddress().getHostAddress());
		if(plugin.notifications != null) {
			plugin.notifications.showNotification(new Notification("[AuthMe] " + player.getName() + " has registered!"));
		}
	}

}
