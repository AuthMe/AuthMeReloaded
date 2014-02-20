package fr.xephi.authme.commands;

import me.muizers.Notifications.Notification;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.Utils;
import fr.xephi.authme.Utils.groupType;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.backup.DataFileCache;
import fr.xephi.authme.cache.backup.FileCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.AuthMeTeleportEvent;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.PlayersLogs;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;


public class LogoutCommand implements CommandExecutor {

    private Messages m = Messages.getInstance();
    private PlayersLogs pllog = PlayersLogs.getInstance();
    private AuthMe plugin;
    private DataSource database;
    private Utils utils = Utils.getInstance();
    private FileCache playerBackup = new FileCache();

    public LogoutCommand(AuthMe plugin, DataSource database) {
        this.plugin = plugin;
        this.database = database;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmnd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        if (!plugin.authmePermissible(sender, "authme." + label.toLowerCase())) {
        	m._(sender, "no_perm");
            return true;
        }

        Player player = (Player) sender;
        String name = player.getName().toLowerCase();

        if (!PlayerCache.getInstance().isAuthenticated(name)) {
        	m._(player, "not_logged_in");
            return true;
        }

        PlayerAuth auth = PlayerCache.getInstance().getAuth(name);
        auth.setIp("198.18.0.1");
        database.updateSession(auth);
        auth.setQuitLocX(player.getLocation().getX());
        auth.setQuitLocY(player.getLocation().getY());
        auth.setQuitLocZ(player.getLocation().getZ());
        auth.setWorld(player.getWorld().getName());
        database.updateQuitLoc(auth);

        PlayerCache.getInstance().removePlayer(name);

        if (Settings.isTeleportToSpawnEnabled) {
        	Location spawnLoc = plugin.getSpawnLocation(name, player.getWorld());
            AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(player, spawnLoc);
            plugin.getServer().getPluginManager().callEvent(tpEvent);
            if(!tpEvent.isCancelled()) {
            	if (!tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).isLoaded()) {
            		tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).load();
            	}
          	  	player.teleport(tpEvent.getTo());
            }
        }

        if (LimboCache.getInstance().hasLimboPlayer(name))
        	LimboCache.getInstance().deleteLimboPlayer(name);
        LimboCache.getInstance().addLimboPlayer(player);
        utils.setGroup(player, groupType.NOTLOGGEDIN);
        if(Settings.protectInventoryBeforeLogInEnabled) {
        	player.getInventory().clear();
            // create cache file for handling lost of inventories on unlogged in status
            DataFileCache playerData = new DataFileCache(LimboCache.getInstance().getLimboPlayer(name).getInventory(),LimboCache.getInstance().getLimboPlayer(name).getArmour());      
            playerBackup.createCache(name, playerData, LimboCache.getInstance().getLimboPlayer(name).getGroup(),LimboCache.getInstance().getLimboPlayer(name).getOperator(),LimboCache.getInstance().getLimboPlayer(name).isFlying());            
        }

        int delay = Settings.getRegistrationTimeout * 20;
        int interval = Settings.getWarnMessageInterval;
        BukkitScheduler sched = sender.getServer().getScheduler();
        if (delay != 0) {
            int id = sched.scheduleSyncDelayedTask(plugin, new TimeoutTask(plugin, name), delay);
            LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id);
        }
        int msgT = sched.scheduleSyncDelayedTask(plugin, new MessageTask(plugin, name, m._("login_msg"), interval));
        LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(msgT);
        try {
	         if (PlayersLogs.players.contains(player.getName())) {
	        	 	PlayersLogs.players.remove(player.getName());
	        	 	pllog.save();
	         }
	         if (player.isInsideVehicle())
	        	 player.getVehicle().eject();
        } catch (NullPointerException npe) {
        }
        m._(player, "logout");
        ConsoleLogger.info(player.getDisplayName() + " logged out");
        if(plugin.notifications != null) {
        	plugin.notifications.showNotification(new Notification("[AuthMe] " + player.getName() + " logged out!"));
        }
        return true;
    }

}
