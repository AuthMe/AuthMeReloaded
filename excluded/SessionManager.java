package fr.xephi.authme.data;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.util.expiring.ExpiringSet;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * Manages sessions, allowing players to be automatically logged in if they join again
 * within a configurable amount of time.
 */
public class SessionManager implements SettingsDependent, HasCleanup {

    private final ExpiringSet<String> sessions;
    private boolean enabled;

    @Inject
    SessionManager(Settings settings) {
        long timeout = settings.getProperty(PluginSettings.SESSIONS_TIMEOUT);
        sessions = new ExpiringSet<>(timeout, TimeUnit.MINUTES);
        enabled = timeout > 0 && settings.getProperty(PluginSettings.SESSIONS_ENABLED);
    }

    /**
     * Check if a session is available for the given player.
     *
     * @param name The name to check.
     * @return True if a session is found.
     */
    private boolean hasSession(String name) {
        return enabled && sessions.contains(name.toLowerCase());
    }

    /**
     * Add a player session to the cache.
     *
     * @param name The name of the player.
     */
    private void addSession(String name) {
        if (enabled) {
            sessions.add(name.toLowerCase());
        }
    }

    /**
     * Remove a player's session from the cache.
     *
     * @param name The name of the player.
     */
    private void removeSession(String name) {
        sessions.remove(name.toLowerCase());
    }

    @Override
    public void reload(Settings settings) {
        long timeoutInMinutes = settings.getProperty(PluginSettings.SESSIONS_TIMEOUT);
        sessions.setExpiration(timeoutInMinutes, TimeUnit.MINUTES);
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
        if (enabled) {
            sessions.removeExpiredEntries();
        }
    }
}
