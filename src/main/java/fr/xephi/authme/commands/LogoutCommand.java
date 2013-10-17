package fr.xephi.authme.commands;

import me.muizers.Notifications.Notification;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.Utils;
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
import fr.xephi.authme.settings.Spawn;
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
            sender.sendMessage(m._("no_perm"));
            return true;
        }

        Player player = (Player) sender;
        String name = player.getName().toLowerCase();

        if (!PlayerCache.getInstance().isAuthenticated(name)) {
            player.sendMessage(m._("not_logged_in"));
            return true;
        }

        PlayerAuth auth = PlayerCache.getInstance().getAuth(name);
        auth.setIp("198.18.0.1");
        database.updateSession(auth);

        PlayerCache.getInstance().removePlayer(name);

        LimboCache.getInstance().addLimboPlayer(player , utils.removeAll(player));
        LimboCache.getInstance().addLimboPlayer(player);
        if(Settings.protectInventoryBeforeLogInEnabled) {
            player.getInventory().setArmorContents(new ItemStack[4]);
            player.getInventory().setContents(new ItemStack[36]);
            // create cache file for handling lost of inventories on unlogged in status
            DataFileCache playerData = new DataFileCache(player.getInventory().getContents(),player.getInventory().getArmorContents());      
            playerBackup.createCache(name, playerData, LimboCache.getInstance().getLimboPlayer(name).getGroup(),LimboCache.getInstance().getLimboPlayer(name).getOperator(),LimboCache.getInstance().getLimboPlayer(name).isFlying());            
        }
        if (Settings.isTeleportToSpawnEnabled) {
        	Location spawnLoc = player.getWorld().getSpawnLocation();
            if (plugin.essentialsSpawn != null) {
            	spawnLoc = plugin.essentialsSpawn;
            }
            if (Spawn.getInstance().getLocation() != null)
            	spawnLoc = Spawn.getInstance().getLocation();
            AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(player, spawnLoc);
            plugin.getServer().getPluginManager().callEvent(tpEvent);
            if(!tpEvent.isCancelled()) {
            	if (!tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).isLoaded()) {
            		tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).load();
            	}
          	  	player.teleport(tpEvent.getTo());
            }
        }

        int delay = Settings.getRegistrationTimeout * 20;
        int interval = Settings.getWarnMessageInterval;
        BukkitScheduler sched = sender.getServer().getScheduler();
        if (delay != 0) {
            BukkitTask id = sched.runTaskLater(plugin, new TimeoutTask(plugin, name), delay);
            LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id.getTaskId());
        }
        BukkitTask msgT = sched.runTask(plugin, new MessageTask(plugin, name, m._("login_msg"), interval));
        LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(msgT.getTaskId());
        try {
	         if (PlayersLogs.players.contains(player.getName())) {
	        	 	PlayersLogs.players.remove(player.getName());
	        	 	pllog.save();
	         }
	         if (player.isInsideVehicle())
	        	 player.getVehicle().eject();
        } catch (NullPointerException npe) {
        }
        player.sendMessage(m._("logout"));
        ConsoleLogger.info(player.getDisplayName() + " logged out");
        if(plugin.notifications != null) {
        	plugin.notifications.showNotification(new Notification("[AuthMe] " + player.getName() + " logged out!"));
        }
        return true;
    }

}
