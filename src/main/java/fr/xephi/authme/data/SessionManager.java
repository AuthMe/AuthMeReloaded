package fr.xephi.authme.data;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static fr.xephi.authme.util.Utils.MILLIS_PER_MINUTE;

/**
 * Manages sessions, allowing players to be automatically logged in if they join again
 * within a configurable amount of time.
 */
public class SessionManager implements SettingsDependent, HasCleanup {

    // Player -> expiration of session in milliseconds
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();

    private boolean enabled;
    private int timeoutInMinutes;

    @Inject
    SessionManager(Settings settings) {
        reload(settings);
    }

    /**
     * Check if a session is available for the given player.
     *
     * @param name The name to check.
     * @return True if a session is found.
     */
    public boolean hasSession(String name) {
        if (enabled) {
            Long timeout = sessions.get(name.toLowerCase());
            if (timeout != null) {
                return System.currentTimeMillis() <= timeout;
            }
        }
        return false;
    }

    /**
     * Add a player session to the cache.
     *
     * @param name The name of the player.
     */
    public void addSession(String name) {
        if (enabled) {
            long timeout = System.currentTimeMillis() + timeoutInMinutes * MILLIS_PER_MINUTE;
            sessions.put(name.toLowerCase(), timeout);
        }
    }

    /**
     * Remove a player's session from the cache.
     *
     * @param name The name of the player.
     */
    public void removeSession(String name) {
        this.sessions.remove(name.toLowerCase());
    }

    @Override
    public void reload(Settings settings) {
        timeoutInMinutes = settings.getProperty(PluginSettings.SESSIONS_TIMEOUT);
        boolean oldEnabled = enabled;
        enabled = timeoutInMinutes > 0 && settings.getProperty(PluginSettings.SESSIONS_ENABLED);

        // With this reload, the sessions feature has just been disabled, so clear all stored sessions
        if (oldEnabled && !enabled) {
            sessions.clear();
            ConsoleLogger.fine("Sessions disabled: cleared all sessions");
        }
    }

    @Override
    public void performCleanup() {
        if (!enabled) {
            return;
        }
        final long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (entry.getValue() < currentTime) {
                iterator.remove();
            }
        }
    }
}
