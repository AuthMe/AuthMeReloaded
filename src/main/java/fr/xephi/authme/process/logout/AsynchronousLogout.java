package fr.xephi.authme.process.logout;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.process.SyncProcessManager;
import fr.xephi.authme.settings.properties.RestrictionSettings;
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

    AsynchronousLogout() {
    }

    public void logout(final Player player) {
        final String name = player.getName().toLowerCase();
        if (!playerCache.isAuthenticated(name)) {
            service.send(player, MessageKey.NOT_LOGGED_IN);
            return;
        }

        PlayerAuth auth = playerCache.getAuth(name);
        database.updateSession(auth);
        if (service.getProperty(RestrictionSettings.SAVE_QUIT_LOCATION)) {
            auth.setQuitLocX(player.getLocation().getX());
            auth.setQuitLocY(player.getLocation().getY());
            auth.setQuitLocZ(player.getLocation().getZ());
            auth.setWorld(player.getWorld().getName());
            database.updateQuitLoc(auth);
        }

        limboCache.addPlayerData(player);
        playerCache.removePlayer(name);
        // TODO LJ: No more teleport here?
        database.setUnlogged(name);
        syncProcessManager.processSyncPlayerLogout(player);
    }
}
