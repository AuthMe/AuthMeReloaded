package fr.xephi.authme.service;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.RestoreSessionEvent;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.util.PlayerUtils;
import org.bukkit.entity.Player;

import javax.inject.Inject;

import static fr.xephi.authme.util.Utils.MILLIS_PER_MINUTE;

/**
 * Handles the user sessions.
 */
public class SessionService implements Reloadable {

    private final CommonService service;
    private final BukkitService bukkitService;
    private final DataSource database;

    private boolean isEnabled;

    @Inject
    SessionService(CommonService service, BukkitService bukkitService, DataSource database) {
        this.service = service;
        this.bukkitService = bukkitService;
        this.database = database;
        reload();
    }

    /**
     * Returns whether the player has a session he can resume.
     *
     * @param player the player to check
     * @return true if there is a current session, false otherwise
     */
    public boolean canResumeSession(Player player) {
        final String name = player.getName();
        if (isEnabled && database.hasSession(name)) {
            database.setUnlogged(name);
            database.revokeSession(name);
            PlayerAuth auth = database.getAuth(name);

            SessionState state = fetchSessionStatus(auth, player);
            if (state.equals(SessionState.VALID)) {
                RestoreSessionEvent event = bukkitService.createAndCallEvent(
                    isAsync -> new RestoreSessionEvent(player, isAsync));
                return !event.isCancelled();
            } else if (state.equals(SessionState.IP_CHANGED)) {
                service.send(player, MessageKey.SESSION_EXPIRED);
            }
        }
        return false;
    }

    /**
     * Checks if the given Player has a current session by comparing its properties
     * with the given PlayerAuth's.
     *
     * @param auth the player auth
     * @param player the associated player
     * @return SessionState based on the state of the session (VALID, NOT_VALID, OUTDATED, IP_CHANGED)
     */
    private SessionState fetchSessionStatus(PlayerAuth auth, Player player) {
        if (auth == null) {
            ConsoleLogger.warning("No PlayerAuth in database for '" + player.getName() + "' during session check");
            return SessionState.NOT_VALID;
        } else if (auth.getLastLogin() == null) {
            return SessionState.NOT_VALID;
        }
        long timeSinceLastLogin = System.currentTimeMillis() - auth.getLastLogin();

        if (timeSinceLastLogin > 0
            && timeSinceLastLogin < service.getProperty(PluginSettings.SESSIONS_TIMEOUT) * MILLIS_PER_MINUTE) {
            if (PlayerUtils.getPlayerIp(player).equals(auth.getLastIp())) {
                return SessionState.VALID;
            } else {
                return SessionState.IP_CHANGED;
            }
        }
        return SessionState.OUTDATED;
    }

    public void grantSession(String name) {
        if (isEnabled) {
            database.grantSession(name);
        }
    }

    public void revokeSession(String name) {
        database.revokeSession(name);
    }

    @Override
    public void reload() {
        this.isEnabled = service.getProperty(PluginSettings.SESSIONS_ENABLED);
    }
}
