package fr.xephi.authme.service;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.RestoreSessionEvent;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.util.PlayerUtils;
import org.bukkit.entity.Player;

import javax.inject.Inject;

/**
 * Handles the user sessions.
 */
public class SessionService {

    private CommonService service;
    private BukkitService bukkitService;
    private DataSource database;

    @Inject
    SessionService(CommonService service, BukkitService bukkitService, DataSource database) {
        this.service = service;
        this.bukkitService = bukkitService;
        this.database = database;
    }

    public boolean canResumeSession(Player player) {
        final String name = player.getName();
        if (service.getProperty(PluginSettings.SESSIONS_ENABLED) && database.hasSession(name)) {
            database.setUnlogged(name);
            database.revokeSession(name);
            PlayerAuth auth = database.getAuth(name);
            if (hasValidSessionData(auth, player)) {
                RestoreSessionEvent event = bukkitService.createAndCallEvent(
                    isAsync -> new RestoreSessionEvent(player, isAsync));
                return !event.isCancelled();
            } else {
                service.send(player, MessageKey.SESSION_EXPIRED);
            }
        }
        return false;
    }

    private boolean hasValidSessionData(PlayerAuth auth, Player player) {
        if (auth == null) {
            ConsoleLogger.warning("No PlayerAuth in database for '" + player.getName() + "' during session check");
            return false;
        }
        long timeSinceLastLogin = System.currentTimeMillis() - auth.getLastLogin();
        return auth.getIp().equals(PlayerUtils.getPlayerIp(player))
            && timeSinceLastLogin > 0
            && timeSinceLastLogin < service.getProperty(PluginSettings.SESSIONS_TIMEOUT) * 60 * 1000;
    }
}
