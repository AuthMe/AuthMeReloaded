package fr.xephi.authme.process.quit;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.SessionManager;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.CacheDataSource;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.process.SyncProcessManager;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.Utils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import javax.inject.Inject;

import static fr.xephi.authme.util.BukkitService.TICKS_PER_MINUTE;

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
    private BukkitService bukkitService;

    AsynchronousQuit() {
    }


    public void processQuit(Player player, boolean isKick) {
        if (player == null || Utils.isUnrestricted(player)) {
            return;
        }
        final String name = player.getName().toLowerCase();

        String ip = Utils.getPlayerIp(player);
        if (playerCache.isAuthenticated(name)) {
            if (service.getProperty(RestrictionSettings.SAVE_QUIT_LOCATION)) {
                Location loc = spawnLoader.getPlayerLocationOrSpawn(player);
                PlayerAuth auth = PlayerAuth.builder()
                    .name(name).location(loc)
                    .realName(player.getName()).build();
                database.updateQuitLoc(auth);
            }
            PlayerAuth auth = PlayerAuth.builder()
                .name(name)
                .realName(player.getName())
                .ip(ip)
                .lastLogin(System.currentTimeMillis())
                .build();
            database.updateSession(auth);
        }

        //always unauthenticate the player - use session only for auto logins on the same ip
        playerCache.removePlayer(name);

        if (plugin.isEnabled() && service.getProperty(PluginSettings.SESSIONS_ENABLED)) {
            BukkitTask task = bukkitService.runTaskLaterAsynchronously(new Runnable() {

                @Override
                public void run() {
                    postLogout(name);
                }

            }, service.getProperty(PluginSettings.SESSIONS_TIMEOUT) * TICKS_PER_MINUTE);

            sessionManager.addSession(name, task);
        } else {
            //plugin is disabled; we cannot schedule more tasks so run it directly here
            postLogout(name);
        }

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

    private void postLogout(String name) {
        sessionManager.removeSession(name);
    }
}
