package fr.xephi.authme.service;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.RestoreSessionEvent;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.output.ConsoleLoggerFactory;
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

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(SessionService.class);
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
            SessionState state = fetchSessionStatus(name, database.getAuth(name), PlayerUtils.getPlayerIp(player));
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
     * Returns whether the given player name has a valid resumable session for the supplied IP address.
     * This check is side-effect free and can be used before a Bukkit {@link Player} instance exists.
     *
     * @param playerName the player name
     * @param ipAddress the player's IP address
     * @return true if the player's session is currently valid, false otherwise
     */
    public boolean hasValidSession(String playerName, String ipAddress) {
        if (!isEnabled || ipAddress == null || !database.hasSession(playerName)) {
            return false;
        }

        return fetchSessionStatus(playerName, database.getAuth(playerName), ipAddress) == SessionState.VALID;
    }

    /**
     * Checks if the given Player has a current session by comparing its properties
     * with the given PlayerAuth's.
     *
     * @param playerName the player name associated with the session check
     * @param auth the player auth
     * @param ipAddress the player's IP address
     * @return SessionState based on the state of the session (VALID, NOT_VALID, OUTDATED, IP_CHANGED)
     */
    private SessionState fetchSessionStatus(String playerName, PlayerAuth auth, String ipAddress) {
        if (auth == null) {
            logger.warning("No PlayerAuth in database for '" + playerName + "' during session check");
            return SessionState.NOT_VALID;
        } else if (auth.getLastLogin() == null) {
            return SessionState.NOT_VALID;
        }
        long timeSinceLastLogin = System.currentTimeMillis() - auth.getLastLogin();

        if (timeSinceLastLogin > 0
            && timeSinceLastLogin < service.getProperty(PluginSettings.SESSIONS_TIMEOUT) * MILLIS_PER_MINUTE) {
            if (ipAddress.equals(auth.getLastIp())) {
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
