package fr.xephi.authme.process.logout;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.process.NewProcess;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.util.Utils;
import fr.xephi.authme.util.Utils.GroupType;
import org.bukkit.entity.Player;

import javax.inject.Inject;

public class AsynchronousLogout implements NewProcess {

    @Inject
    private AuthMe plugin;

    @Inject
    private DataSource database;

    @Inject
    private ProcessService service;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private LimboCache limboCache;

    AsynchronousLogout() { }

    public void logout(Player player) {
        final String name = player.getName().toLowerCase();
        if (!playerCache.isAuthenticated(name)) {
            service.send(player, MessageKey.NOT_LOGGED_IN);
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

        playerCache.removePlayer(name);
        database.setUnlogged(name);
        service.scheduleSyncDelayedTask(new Runnable() {
            @Override
            public void run() {
                Utils.teleportToSpawn(p);
            }
        });
        if (limboCache.hasLimboPlayer(name)) {
            limboCache.deleteLimboPlayer(name);
        }
        limboCache.addLimboPlayer(player);
        Utils.setGroup(player, GroupType.NOTLOGGEDIN);
        service.scheduleSyncDelayedTask(new ProcessSynchronousPlayerLogout(p, plugin, service));
    }
}
