package fr.xephi.authme.service;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.player.NamedIdentifier;
import fr.xephi.authme.data.player.OnlineIdentifier;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.RestoreSessionEvent;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.util.PlayerUtils;

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
     * @param identifier the player to check
     *
     * @return true if there is a current session, false otherwise
     */
    public boolean canResumeSession(OnlineIdentifier identifier) {
        if (isEnabled && database.hasSession(identifier)) {
            database.setUnlogged(identifier);
            database.revokeSession(identifier);
            PlayerAuth auth = database.getAuth(identifier);

            SessionState state = fetchSessionStatus(auth, identifier);
            if (state.equals(SessionState.VALID)) {
                RestoreSessionEvent event = bukkitService.createAndCallEvent(
                    isAsync -> new RestoreSessionEvent(identifier, isAsync));
                return !event.isCancelled();
            } else if (state.equals(SessionState.IP_CHANGED)) {
                service.send(identifier, MessageKey.SESSION_EXPIRED);
            }
        }
        return false;
    }

    /**
     * Checks if the given Player has a current session by comparing its properties
     * with the given PlayerAuth's.
     *
     * @param auth   the player auth
     * @param identifier the associated player identifier
     *
     * @return SessionState based on the state of the session (VALID, NOT_VALID, OUTDATED, IP_CHANGED)
     */
    private SessionState fetchSessionStatus(PlayerAuth auth, OnlineIdentifier identifier) {
        if (auth == null) {
            ConsoleLogger.warning("No PlayerAuth in database for '" + identifier.getRealName() + "' during session check");
            return SessionState.NOT_VALID;
        } else if (auth.getLastLogin() == null) {
            return SessionState.NOT_VALID;
        }
        long timeSinceLastLogin = System.currentTimeMillis() - auth.getLastLogin();

        if (timeSinceLastLogin > 0
            && timeSinceLastLogin < service.getProperty(PluginSettings.SESSIONS_TIMEOUT) * MILLIS_PER_MINUTE) {
            if (PlayerUtils.getPlayerIp(identifier.getPlayer()).equals(auth.getLastIp())) {
                return SessionState.VALID;
            } else {
                return SessionState.IP_CHANGED;
            }
        }
        return SessionState.OUTDATED;
    }

    public void grantSession(NamedIdentifier identifier) {
        if (isEnabled) {
            database.grantSession(identifier);
        }
    }

    public void revokeSession(NamedIdentifier identifier) {
        database.revokeSession(identifier);
    }

    @Override
    public void reload() {
        this.isEnabled = service.getProperty(PluginSettings.SESSIONS_ENABLED);
    }
}
