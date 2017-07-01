package fr.xephi.authme.data.limbo;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.bukkit.entity.Player;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;

/**
 * Changes the permission group according to the auth status of the player and the configuration.
 * <p>
 * If this feature is enabled, the <i>primary permissions group</i> of a player is replaced until he has
 * logged in. Some permission plugins have a notion of a primary group; for other permission plugins the
 * first group is simply taken.
 * <p>
 * The groups that are used as replacement until the player logs in is configurable and depends on if
 * the player is registered or not. Note that some (all?) permission systems require the group to actually
 * exist for the replacement to take place. Furthermore, since some permission groups require that players
 * be in at least one group, this will mean that the player is not removed from his primary group.
 */
class AuthGroupHandler implements Reloadable {

    @Inject
    private PermissionsManager permissionsManager;

    @Inject
    private Settings settings;

    private String unregisteredGroup;
    private String registeredGroup;

    AuthGroupHandler() {
    }

    /**
     * Sets the group of a player by its authentication status.
     *
     * @param player the player
     * @param limbo the associated limbo player (nullable)
     * @param groupType the group type
     */
    void setGroup(Player player, LimboPlayer limbo, AuthGroupType groupType) {
        if (!useAuthGroups()) {
            return;
        }

        Collection<String> previousGroups = limbo == null ? Collections.emptyList() : limbo.getGroups();

        switch (groupType) {
            // Implementation note: some permission systems don't support players not being in any group,
            // so add the new group before removing the old ones
            case UNREGISTERED:
                permissionsManager.addGroup(player, unregisteredGroup);
                permissionsManager.removeGroup(player, registeredGroup);
                permissionsManager.removeGroups(player, previousGroups);
                break;

            case REGISTERED_UNAUTHENTICATED:
                permissionsManager.addGroup(player, registeredGroup);
                permissionsManager.removeGroup(player, unregisteredGroup);
                permissionsManager.removeGroups(player, previousGroups);

                break;

            case LOGGED_IN:
                permissionsManager.addGroups(player, previousGroups);
                permissionsManager.removeGroup(player, unregisteredGroup);
                permissionsManager.removeGroup(player, registeredGroup);
                break;

            default:
                throw new IllegalStateException("Encountered unhandled auth group type '" + groupType + "'");
        }

        ConsoleLogger.debug(() -> player.getName() + " changed to "
            + groupType + ": has groups " + permissionsManager.getGroups(player));
    }

    /**
     * Returns whether the auth permissions group function should be used.
     *
     * @return true if should be used, false otherwise
     */
    private boolean useAuthGroups() {
        // Check whether the permissions check is enabled
        if (!settings.getProperty(PluginSettings.ENABLE_PERMISSION_CHECK)) {
            return false;
        }

        // Make sure group support is available
        if (!permissionsManager.hasGroupSupport()) {
            ConsoleLogger.warning("The current permissions system doesn't have group support, unable to set group!");
            return false;
        }
        return true;
    }

    @Override
    @PostConstruct
    public void reload() {
        unregisteredGroup = settings.getProperty(PluginSettings.UNREGISTERED_GROUP);
        registeredGroup = settings.getProperty(PluginSettings.REGISTERED_GROUP);
    }

}
