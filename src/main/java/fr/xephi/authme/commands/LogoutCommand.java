package fr.xephi.authme.commands;

import me.muizers.Notifications.Notification;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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
import fr.xephi.authme.events.LogoutEvent;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;

public class LogoutCommand implements CommandExecutor {

    private Messages m = Messages.getInstance();
    private AuthMe plugin;
    private DataSource database;
    private Utils utils = Utils.getInstance();
    private FileCache playerBackup;

    public LogoutCommand(AuthMe plugin, DataSource database) {
        this.plugin = plugin;
        this.database = database;
        this.playerBackup = new FileCache(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmnd, String label,
            String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        if (!plugin.authmePermissible(sender, "authme." + label.toLowerCase())) {
            m._(sender, "no_perm");
            return true;
        }

        final Player player = (Player) sender;
        String name = player.getName();

        if (!PlayerCache.getInstance().isAuthenticated(name)) {
            m._(player, "not_logged_in");
            return true;
        }

        PlayerAuth auth = PlayerCache.getInstance().getAuth(name);
        if (Settings.isSessionsEnabled)
            auth.setLastLogin(0L);
        database.updateSession(auth);
        auth.setQuitLocX(player.getLocation().getX());
        auth.setQuitLocY(player.getLocation().getY());
        auth.setQuitLocZ(player.getLocation().getZ());
        auth.setWorld(player.getWorld().getName());
        database.updateQuitLoc(auth);

        PlayerCache.getInstance().removePlayer(name);
        database.setUnlogged(name);

        if (Settings.isTeleportToSpawnEnabled && !Settings.noTeleport) {
            Location spawnLoc = plugin.getSpawnLocation(player);
            AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(player, spawnLoc);
            plugin.getServer().getPluginManager().callEvent(tpEvent);
            if (!tpEvent.isCancelled()) {
                if (tpEvent.getTo() != null)
                    player.teleport(tpEvent.getTo());
            }
        }

        if (LimboCache.getInstance().hasLimboPlayer(name))
            LimboCache.getInstance().deleteLimboPlayer(name);
        LimboCache.getInstance().addLimboPlayer(player);
        utils.setGroup(player, groupType.NOTLOGGEDIN);
        if (Settings.protectInventoryBeforeLogInEnabled) {
            player.getInventory().clear();
            // create cache file for handling lost of inventories on unlogged in
            // status
            DataFileCache playerData = new DataFileCache(LimboCache.getInstance().getLimboPlayer(name).getInventory(), LimboCache.getInstance().getLimboPlayer(name).getArmour());
            playerBackup.createCache(player, playerData, LimboCache.getInstance().getLimboPlayer(name).getGroup(), LimboCache.getInstance().getLimboPlayer(name).getOperator(), LimboCache.getInstance().getLimboPlayer(name).isFlying());
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
            if (player.isInsideVehicle())
                player.getVehicle().eject();
        } catch (NullPointerException npe) {
        }
        if (Settings.applyBlindEffect)
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Settings.getRegistrationTimeout * 20, 2));
        player.setOp(false);
        player.setAllowFlight(true);
        player.setFlying(true);
        // Player is now logout... Time to fire event !
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){
			@Override
            public void run() {
				Bukkit.getServer().getPluginManager().callEvent(new LogoutEvent(player));
            }
        });
        m._(player, "logout");
        ConsoleLogger.info(player.getDisplayName() + " logged out");
        if (plugin.notifications != null) {
            plugin.notifications.showNotification(new Notification("[AuthMe] " + player.getName() + " logged out!"));
        }
        return true;
    }

}
