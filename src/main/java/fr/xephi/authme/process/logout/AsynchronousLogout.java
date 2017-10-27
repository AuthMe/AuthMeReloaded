package fr.xephi.authme.process.logout;

import fr.xephi.authme.data.VerificationCodeManager;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.SyncProcessManager;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.entity.Player;

import javax.inject.Inject;

/**
 * Async task when a player wants to log out.
 */
public class AsynchronousLogout implements AsynchronousProcess {

    @Inject
    private DataSource database;

    @Inject
    private CommonService service;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private VerificationCodeManager codeManager;

    @Inject
    private SyncProcessManager syncProcessManager;

    AsynchronousLogout() {
    }

    /**
     * Handles a player's request to log out.
     *
     * @param player the player wanting to log out
     */
    public void logout(Player player) {
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

        playerCache.removePlayer(name);
        codeManager.unverify(name);
        database.setUnlogged(name);
        database.revokeSession(name);
        syncProcessManager.processSyncPlayerLogout(player);
    }
}
