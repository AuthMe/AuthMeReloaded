package fr.xephi.authme.process.logout;

import fr.xephi.authme.cache.SessionManager;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.process.SyncProcessManager;
import fr.xephi.authme.settings.properties.PluginSettings;
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
    private SessionManager sessionManager;

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
            auth.setQuitLocation(player.getLocation());
            database.updateQuitLoc(auth);
        }

        if (service.getProperty(PluginSettings.SESSIONS_ENABLED) && (sessionManager.hasSession(name) || database.isLogged(name))) {
            sessionManager.cancelSession(name);
            service.send(player, MessageKey.SESSION_EXPIRED);
        }

        limboCache.addPlayerData(player);
        playerCache.removePlayer(name);
        database.setUnlogged(name);
        syncProcessManager.processSyncPlayerLogout(player);
    }
}
