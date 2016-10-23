package fr.xephi.authme.process.quit;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.data.SessionManager;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.CacheDataSource;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.process.SyncProcessManager;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.PlayerUtils;
import fr.xephi.authme.service.ValidationService;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.inject.Inject;

public class AsynchronousQuit implements AsynchronousProcess {

    @Inject
    private AuthMe plugin;

    @Inject
    private DataSource database;

    @Inject
    private ProcessService service;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private SyncProcessManager syncProcessManager;

    @Inject
    private SessionManager sessionManager;

    @Inject
    private SpawnLoader spawnLoader;

    @Inject
    private ValidationService validationService;

    AsynchronousQuit() {
    }


    public void processQuit(Player player) {
        if (player == null || validationService.isUnrestricted(player.getName())) {
            return;
        }
        final String name = player.getName().toLowerCase();

        if (playerCache.isAuthenticated(name)) {
            if (service.getProperty(RestrictionSettings.SAVE_QUIT_LOCATION)) {
                Location loc = spawnLoader.getPlayerLocationOrSpawn(player);
                PlayerAuth auth = PlayerAuth.builder()
                    .name(name).location(loc)
                    .realName(player.getName()).build();
                database.updateQuitLoc(auth);
            }

            final String ip = PlayerUtils.getPlayerIp(player);
            PlayerAuth auth = PlayerAuth.builder()
                .name(name)
                .realName(player.getName())
                .ip(ip)
                .lastLogin(System.currentTimeMillis())
                .build();
            database.updateSession(auth);

            sessionManager.addSession(name);
        }

        //always unauthenticate the player - use session only for auto logins on the same ip
        playerCache.removePlayer(name);

        //always update the database when the player quit the game
        database.setUnlogged(name);

        if (plugin.isEnabled()) {
            syncProcessManager.processSyncPlayerQuit(player);
        }
        // remove player from cache
        if (database instanceof CacheDataSource) {
            ((CacheDataSource) database).getCachedAuths().invalidate(name);
        }
    }

}
