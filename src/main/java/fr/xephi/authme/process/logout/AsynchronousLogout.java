package fr.xephi.authme.process.logout;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.process.SyncProcessManager;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.Utils;
import org.bukkit.entity.Player;

import javax.inject.Inject;

public class AsynchronousLogout implements AsynchronousProcess {

    @Inject
    private DataSource database;

    @Inject
    private ProcessService service;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private LimboCache limboCache;

    @Inject
    private SyncProcessManager syncProcessManager;

    @Inject
    private BukkitService bukkitService;

    AsynchronousLogout() { }

    public void logout(final Player player) {
        final String name = player.getName().toLowerCase();
        if (!playerCache.isAuthenticated(name)) {
            service.send(player, MessageKey.NOT_LOGGED_IN);
            return;
        }
        PlayerAuth auth = playerCache.getAuth(name);
        database.updateSession(auth);
        auth.setQuitLocX(player.getLocation().getX());
        auth.setQuitLocY(player.getLocation().getY());
        auth.setQuitLocZ(player.getLocation().getZ());
        auth.setWorld(player.getWorld().getName());
        database.updateQuitLoc(auth);

        playerCache.removePlayer(name);
        database.setUnlogged(name);
        bukkitService.scheduleSyncDelayedTask(new Runnable() {
            @Override
            public void run() {
                Utils.teleportToSpawn(player);
            }
        });
        limboCache.updateLimboPlayer(player);
        syncProcessManager.processSyncPlayerLogout(player);
    }
}
