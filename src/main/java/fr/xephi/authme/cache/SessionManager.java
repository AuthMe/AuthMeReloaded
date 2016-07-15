package fr.xephi.authme.cache;

import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.PluginSettings;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages sessions, allowing players to be automatically logged in if they join again
 * within a configurable amount of time.
 */
public class SessionManager implements SettingsDependent {

    private static final int MINUTE_IN_MILLIS = 60_000;
    // Player -> expiration of session in milliseconds
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();

    private boolean enabled;
    private int timeoutInMinutes;

    @Inject
    SessionManager(NewSetting settings) {
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
            long timeout = System.currentTimeMillis() + timeoutInMinutes * MINUTE_IN_MILLIS;
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
    public void reload(NewSetting settings) {
        timeoutInMinutes = settings.getProperty(PluginSettings.SESSIONS_TIMEOUT);
        enabled = timeoutInMinutes > 0 && settings.getProperty(PluginSettings.SESSIONS_ENABLED);
    }
}
