package fr.xephi.authme.permission;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.limbo.LimboCache;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.bukkit.entity.Player;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Changes the permission group according to the auth status of the player and the configuration.
 */
public class AuthGroupHandler implements Reloadable {

    @Inject
    private PermissionsManager permissionsManager;

    @Inject
    private Settings settings;

    @Inject
    private LimboCache limboCache;

    private String unregisteredGroup;
    private String registeredGroup;

    AuthGroupHandler() {
    }

    /**
     * Sets the group of a player by its authentication status.
     *
     * @param player the player
     * @param groupType the group type
     *
     * @return True upon success, false otherwise. False is also returned if groups aren't supported
     * with the current permissions system.
     */
    public boolean setGroup(Player player, AuthGroupType groupType) {
        // Check whether the permissions check is enabled
        if (!settings.getProperty(PluginSettings.ENABLE_PERMISSION_CHECK)) {
            return false;
        }

        // Make sure group support is available
        if (!permissionsManager.hasGroupSupport()) {
            ConsoleLogger.warning("The current permissions system doesn't have group support, unable to set group!");
            return false;
        }

        switch (groupType) {
            case UNREGISTERED:
                // Remove the other group, set the current group
                permissionsManager.removeGroups(player, registeredGroup);
                return permissionsManager.addGroup(player, unregisteredGroup);

            case REGISTERED_UNAUTHENTICATED:
                // Remove the other group, set the current group
                permissionsManager.removeGroups(player, unregisteredGroup);
                return permissionsManager.addGroup(player, registeredGroup);

            case LOGGED_IN:
                return restoreGroup(player);

            default:
                throw new IllegalStateException("Encountered unhandled auth group type '" + groupType + "'");
        }
    }

    private boolean restoreGroup(Player player) {
        // Get the player's LimboPlayer
        LimboPlayer limbo = limboCache.getPlayerData(player.getName());
        if (limbo == null) {
            return false;
        }

        // Get the players group
        String realGroup = limbo.getGroup();

        // Remove the other group types groups, set the real group
        permissionsManager.removeGroups(player, unregisteredGroup, registeredGroup);
        return permissionsManager.addGroup(player, realGroup);
    }

    @Override
    @PostConstruct
    public void reload() {
        unregisteredGroup = settings.getProperty(PluginSettings.UNREGISTERED_GROUP);
        registeredGroup = settings.getProperty(PluginSettings.REGISTERED_GROUP);
    }

}
