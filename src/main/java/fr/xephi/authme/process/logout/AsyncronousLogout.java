package fr.xephi.authme.process.logout;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.Utils;
import fr.xephi.authme.Utils.GroupType;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.backup.DataFileCache;
import fr.xephi.authme.cache.backup.JsonCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.AuthMeTeleportEvent;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;

public class AsyncronousLogout {

    protected Player player;
    protected String name;
    protected AuthMe plugin;
    protected DataSource database;
    protected boolean canLogout = true;
    private Messages m = Messages.getInstance();
    private JsonCache playerBackup;

    public AsyncronousLogout(Player player, AuthMe plugin,
            DataSource database) {
        this.player = player;
        this.plugin = plugin;
        this.database = database;
        this.name = player.getName().toLowerCase();
        this.playerBackup = new JsonCache(plugin);
    }

    private void preLogout() {
        if (!PlayerCache.getInstance().isAuthenticated(name)) {
            m.send(player, "not_logged_in");
            canLogout = false;
        }
    }

    public void process() {
        preLogout();
        if (!canLogout)
            return;
        final Player p = player;
        BukkitScheduler sched = p.getServer().getScheduler();
        PlayerAuth auth = PlayerCache.getInstance().getAuth(name);
        database.updateSession(auth);
        auth.setQuitLocX(p.getLocation().getX());
        auth.setQuitLocY(p.getLocation().getY());
        auth.setQuitLocZ(p.getLocation().getZ());
        auth.setWorld(p.getWorld().getName());
        database.updateQuitLoc(auth);

        PlayerCache.getInstance().removePlayer(name);
        database.setUnlogged(name);
        if (Settings.isTeleportToSpawnEnabled && !Settings.noTeleport) {
            Location spawnLoc = plugin.getSpawnLocation(p);
            final AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(p, spawnLoc);
            sched.scheduleSyncDelayedTask(plugin, new Runnable() {

                @Override
                public void run() {
                    plugin.getServer().getPluginManager().callEvent(tpEvent);
                    if (!tpEvent.isCancelled()) {
                        if (tpEvent.getTo() != null)
                            p.teleport(tpEvent.getTo());
                    }
                }
            });
        }

        if (LimboCache.getInstance().hasLimboPlayer(name))
            LimboCache.getInstance().deleteLimboPlayer(name);
        LimboCache.getInstance().addLimboPlayer(player);
        Utils.setGroup(player, GroupType.NOTLOGGEDIN);
        if (Settings.protectInventoryBeforeLogInEnabled) {
            player.getInventory().clear();
            // create cache file for handling lost of inventories on unlogged in
            // status
            DataFileCache playerData = new DataFileCache(LimboCache.getInstance().getLimboPlayer(name).getInventory(), LimboCache.getInstance().getLimboPlayer(name).getArmour());
            playerBackup.createCache(player, playerData);
        }
        sched.scheduleSyncDelayedTask(plugin, new ProcessSyncronousPlayerLogout(p, plugin));
    }
}
