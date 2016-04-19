package fr.xephi.authme.process.logout;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.process.Process;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.util.Utils;
import fr.xephi.authme.util.Utils.GroupType;
import org.bukkit.entity.Player;

/**
 */
public class AsynchronousLogout implements Process {

    private final Player player;
    private final String name;
    private final AuthMe plugin;
    private final DataSource database;
    private boolean canLogout = true;
    private final ProcessService service;

    /**
     * Constructor for AsynchronousLogout.
     *
     * @param player   Player
     * @param plugin   AuthMe
     * @param database DataSource
     * @param service  The process service
     */
    public AsynchronousLogout(Player player, AuthMe plugin, DataSource database, ProcessService service) {
        this.player = player;
        this.plugin = plugin;
        this.database = database;
        this.name = player.getName().toLowerCase();
        this.service = service;
    }

    private void preLogout() {
        if (!PlayerCache.getInstance().isAuthenticated(name)) {
            service.send(player, MessageKey.NOT_LOGGED_IN);
            canLogout = false;
        }
    }

    @Override
    public void run() {
        preLogout();
        if (!canLogout) {
            return;
        }
        final Player p = player;
        PlayerAuth auth = PlayerCache.getInstance().getAuth(name);
        database.updateSession(auth);
        auth.setQuitLocX(p.getLocation().getX());
        auth.setQuitLocY(p.getLocation().getY());
        auth.setQuitLocZ(p.getLocation().getZ());
        auth.setWorld(p.getWorld().getName());
        database.updateQuitLoc(auth);

        PlayerCache.getInstance().removePlayer(name);
        database.setUnlogged(name);
        service.scheduleSyncDelayedTask(new Runnable() {
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
        service.scheduleSyncDelayedTask(new ProcessSynchronousPlayerLogout(p, plugin, service));
    }
}
