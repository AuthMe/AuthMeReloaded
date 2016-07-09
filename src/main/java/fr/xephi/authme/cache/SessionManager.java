package fr.xephi.authme.cache;

import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.bukkit.scheduler.BukkitTask;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager implements SettingsDependent {

    private final ConcurrentHashMap<String, BukkitTask> sessions = new ConcurrentHashMap<>();

    private boolean enabled;
    private int sessionTimeout;

    @Inject
    SessionManager(NewSetting settings) {
        reload(settings);
    }

    /**
     * Check if a session for a player is currently being cached.
     *
     * @param name The name to check.
     * @return True if a session is found.
     */
    public boolean hasSession(String name) {
        return enabled && sessions.containsKey(name);
    }

    /**
     * Add a player session to the cache.
     *
     * @param name The name of the player.
     * @param task The task to run.
     */
    public void addSession(String name, BukkitTask task) {
        if (!enabled || sessionTimeout == 0) {
            return;
        }

        this.sessions.put(name, task);
    }

    /**
     * Cancels a player's session. After the task is cancelled, it will be removed from
     * the cache.
     *
     * @param name The name of the player who's session to cancel.
     */
    public void cancelSession(String name) {
        BukkitTask task = sessions.remove(name);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Remove a player's session from the cache.
     *
     * @param name The name of the player.
     */
    public void removeSession(String name) {
        this.sessions.remove(name);
    }

    @Override
    public void reload(NewSetting settings) {
        this.enabled = settings.getProperty(PluginSettings.SESSIONS_ENABLED);
        this.sessionTimeout = settings.getProperty(PluginSettings.SESSIONS_TIMEOUT);
    }
}
