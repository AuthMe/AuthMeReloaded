package fr.xephi.authme.listener;

import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.hooks.PluginHooks;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

/**
 * Service class for the AuthMe listeners to determine whether an event should be canceled.
 */
class ListenerService implements SettingsDependent {

    private final DataSource dataSource;
    private final PluginHooks pluginHooks;
    private final PlayerCache playerCache;

    private boolean isRegistrationForced;
    private Set<String> unrestrictedNames;

    @Inject
    ListenerService(NewSetting settings, DataSource dataSource, PluginHooks pluginHooks, PlayerCache playerCache) {
        this.dataSource = dataSource;
        this.pluginHooks = pluginHooks;
        this.playerCache = playerCache;
        loadSettings(settings);
    }

    /**
     * Returns whether an event should be canceled (for unauthenticated, non-NPC players).
     *
     * @param event the event to process
     * @return true if the event should be canceled, false otherwise
     */
    public boolean shouldCancelEvent(EntityEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || !(entity instanceof Player)) {
            return false;
        }

        Player player = (Player) entity;
        return shouldCancelEvent(player);
    }

    /**
     * Returns whether an event should be canceled (for unauthenticated, non-NPC players).
     *
     * @param event the event to process
     * @return true if the event should be canceled, false otherwise
     */
    public boolean shouldCancelEvent(PlayerEvent event) {
        Player player = event.getPlayer();
        return shouldCancelEvent(player);
    }

    /**
     * Returns, based on the player associated with the event, whether or not the event should be canceled.
     *
     * @param player the player to verify
     * @return true if the associated event should be canceled, false otherwise
     */
    public boolean shouldCancelEvent(Player player) {
        return player != null && !checkAuth(player.getName()) && !pluginHooks.isNpc(player);
    }

    @Override
    public void loadSettings(NewSetting settings) {
        isRegistrationForced = settings.getProperty(RegistrationSettings.FORCE);
        // Keep unrestricted names as Set for more efficient contains()
        unrestrictedNames = new HashSet<>(settings.getProperty(RestrictionSettings.UNRESTRICTED_NAMES));
    }

    /**
     * Checks whether the player is allowed to perform actions (i.e. whether he is logged in
     * or if other settings permit playing).
     *
     * @param name the name of the player to verify
     * @return true if the player may play, false otherwise
     */
    private boolean checkAuth(String name) {
        if (isUnrestricted(name) || playerCache.isAuthenticated(name)) {
            return true;
        }
        if (!isRegistrationForced && !dataSource.isAuthAvailable(name)) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the name is unrestricted according to the configured settings.
     *
     * @param name the name to verify
     * @return true if unrestricted, false otherwise
     */
    private boolean isUnrestricted(String name) {
        return unrestrictedNames.contains(name.toLowerCase());
    }
}
