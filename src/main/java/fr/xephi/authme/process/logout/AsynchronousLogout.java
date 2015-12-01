package fr.xephi.authme.process.logout;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.util.Utils;
import fr.xephi.authme.util.Utils.GroupType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

/**
 */
public class AsynchronousLogout {

    protected final Player player;
    protected final String name;
    protected final AuthMe plugin;
    protected final DataSource database;
    protected boolean canLogout = true;
    private final Messages m;

    /**
     * Constructor for AsynchronousLogout.
     *
     * @param player   Player
     * @param plugin   AuthMe
     * @param database DataSource
     */
    public AsynchronousLogout(Player player, AuthMe plugin, DataSource database) {
        this.m = plugin.getMessages();
        this.player = player;
        this.plugin = plugin;
        this.database = database;
        this.name = player.getName().toLowerCase();
    }

    private void preLogout() {
        if (!PlayerCache.getInstance().isAuthenticated(name)) {
            m.send(player, MessageKey.NOT_LOGGED_IN);
            canLogout = false;
        }
    }

    public void process() {
        preLogout();
        if (!canLogout) {
            return;
        }
        final Player p = player;
        BukkitScheduler scheduler = p.getServer().getScheduler();
        PlayerAuth auth = PlayerCache.getInstance().getAuth(name);
        database.updateSession(auth);
        auth.setQuitLocX(p.getLocation().getX());
        auth.setQuitLocY(p.getLocation().getY());
        auth.setQuitLocZ(p.getLocation().getZ());
        auth.setWorld(p.getWorld().getName());
        database.updateQuitLoc(auth);

        PlayerCache.getInstance().removePlayer(name);
        database.setUnlogged(name);
        scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                Utils.teleportToSpawn(p);
            }
        });
        if (LimboCache.getInstance().hasLimboPlayer(name)) {
            LimboCache.getInstance().deleteLimboPlayer(name);
        }
        LimboCache.getInstance().addLimboPlayer(player);
        Utils.setGroup(player, GroupType.NOTLOGGEDIN);
        scheduler.scheduleSyncDelayedTask(plugin, new ProcessSyncronousPlayerLogout(p, plugin));
    }
}
