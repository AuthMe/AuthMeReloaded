package fr.xephi.authme.process.quit;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.ProxySessionManager;
import fr.xephi.authme.data.VerificationCodeManager;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.data.limbo.LimboPlayerState;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.SyncProcessManager;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.DialogStateService;
import fr.xephi.authme.service.PremiumLoginVerifier;
import fr.xephi.authme.service.SessionService;
import fr.xephi.authme.service.TeleportationService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Locale;

/**
 * Async process called when a player quits the server.
 */
public class AsynchronousQuit implements AsynchronousProcess {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(AsynchronousQuit.class);

    @Inject
    private AuthMe plugin;

    @Inject
    private DataSource database;

    @Inject
    private CommonService service;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private SyncProcessManager syncProcessManager;

    @Inject
    private SpawnLoader spawnLoader;

    @Inject
    private ValidationService validationService;

    @Inject
    private VerificationCodeManager codeManager;

    @Inject
    private SessionService sessionService;

    @Inject
    private TeleportationService teleportationService;

    @Inject
    private DialogStateService dialogStateService;

    @Inject
    private ProxySessionManager proxySessionManager;

    @Inject
    private PremiumLoginVerifier premiumLoginVerifier;

    @Inject
    private LimboService limboService;

    AsynchronousQuit() {
    }

    /**
     * Processes that the given player has quit the server.
     *
     * @param player the player who left
     */
    public void processQuit(Player player) {
        if (player == null || validationService.isUnrestricted(player.getName())) {
            return;
        }
        String name = player.getName().toLowerCase(Locale.ROOT);
        teleportationService.clearOriginalJoinLocation(name);
        dialogStateService.clearDialogOpen(player);
        // Drop any name-keyed auto-login grant so it cannot be inherited by a later connection using this name
        proxySessionManager.removeLoginRequest(name);
        premiumLoginVerifier.removeVerified(name);
        // A "password accepted, 2FA code pending" state belongs to this connection only. On Folia the sync
        // quit that normally discards the in-memory limbo never runs (the player's scheduler is retired), so
        // reset it: the next connection with this name must provide the password again.
        LimboPlayer limbo = limboService.getLimboPlayer(name);
        if (limbo != null) {
            limbo.setState(LimboPlayerState.PASSWORD_REQUIRED);
        }
        boolean wasLoggedIn = playerCache.isAuthenticated(name);

        try {
            if (wasLoggedIn) {
                saveQuitState(player, name);
            }
        } finally {
            // Security-critical: the player must end up unauthenticated even if a database write or the
            // quit-location lookup above threw. A leftover PlayerCache entry would let anyone joining with
            // this name in without a password on offline-mode servers.
            playerCache.removePlayer(name);
            codeManager.unverify(name);

            //always update the database when the player quit the game (if sessions are disabled)
            if (wasLoggedIn) {
                database.setUnlogged(name);
                if (!service.getProperty(PluginSettings.SESSIONS_ENABLED)) {
                    sessionService.revokeSession(name);
                }
            }

            if (plugin.isEnabled()) {
                syncProcessManager.processSyncPlayerQuit(player, wasLoggedIn);
            }

            // remove player from cache
            database.invalidateCache(name);
        }
    }

    private void saveQuitState(Player player, String name) {
        if (service.getProperty(RestrictionSettings.SAVE_QUIT_LOCATION)) {
            try {
                Location loc = spawnLoader.getPlayerLocationOrSpawn(player);
                PlayerAuth auth = PlayerAuth.builder()
                    .name(name).location(loc)
                    .realName(player.getName()).build();
                database.updateQuitLoc(auth);
            } catch (RuntimeException e) {
                // On Folia, reading the respawn location of a player who quit while dead can throw; the
                // quit-location save is best-effort and must never abort the rest of the quit processing.
                logger.warning("Could not save quit location for '" + name + "': " + e.getMessage());
            }
        }

        String ip = PlayerUtils.getPlayerIp(player);
        PlayerAuth auth = PlayerAuth.builder()
            .name(name)
            .realName(player.getName())
            .lastIp(ip)
            .lastLogin(System.currentTimeMillis())
            .build();
        database.updateSession(auth);

        // TODO: send an update when a messaging service will be implemented (QUITLOC)
    }

}
