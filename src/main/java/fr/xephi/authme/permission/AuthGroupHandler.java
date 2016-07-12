package fr.xephi.authme.permission;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.PlayerData;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Arrays;

/**
 * Changes the permission group according to the auth status of the player and the configuration.
 */
public class AuthGroupHandler {

    @Inject
    private PermissionsManager permissionsManager;

    @Inject
    private NewSetting settings;

    @Inject
    private LimboCache limboCache;

    AuthGroupHandler() { }

    /**
     * Set the group of a player, by its AuthMe group type.
     *
     * @param player The player.
     * @param group  The group type.
     *
     * @return True if succeeded, false otherwise. False is also returned if groups aren't supported
     * with the current permissions system.
     */
    public boolean setGroup(Player player, AuthGroupType group) {
        // Check whether the permissions check is enabled
        if (!settings.getProperty(PluginSettings.ENABLE_PERMISSION_CHECK)) {
            return false;
        }

        // Make sure group support is available
        if (!permissionsManager.hasGroupSupport()) {
            ConsoleLogger.warning("The current permissions system doesn't have group support, unable to set group!");
            return false;
        }

        switch (group) {
            case UNREGISTERED:
                // Remove the other group type groups, set the current group
                permissionsManager.removeGroups(player, Arrays.asList(Settings.getRegisteredGroup, settings.getProperty(SecuritySettings.UNLOGGEDIN_GROUP)));
                return permissionsManager.addGroup(player, Settings.unRegisteredGroup);

            case REGISTERED:
                // Remove the other group type groups, set the current group
                permissionsManager.removeGroups(player, Arrays.asList(Settings.unRegisteredGroup, settings.getProperty(SecuritySettings.UNLOGGEDIN_GROUP)));
                return permissionsManager.addGroup(player, Settings.getRegisteredGroup);

            case NOT_LOGGED_IN:
                // Remove the other group type groups, set the current group
                permissionsManager.removeGroups(player, Arrays.asList(Settings.unRegisteredGroup, Settings.getRegisteredGroup));
                return permissionsManager.addGroup(player, settings.getProperty(SecuritySettings.UNLOGGEDIN_GROUP));

            case LOGGED_IN:
                // Get the player data
                PlayerData data = limboCache.getPlayerData(player.getName().toLowerCase());
                if (data == null) {
                    return false;
                }

                // Get the players group
                String realGroup = data.getGroup();

                // Remove the other group types groups, set the real group
                permissionsManager.removeGroups(player,
                    Arrays.asList(Settings.unRegisteredGroup, Settings.getRegisteredGroup, settings.getProperty(SecuritySettings.UNLOGGEDIN_GROUP))
                );
                return permissionsManager.addGroup(player, realGroup);
            default:
                return false;
        }
    }

    /**
     * TODO: This method requires better explanation.
     * <p>
     * Set the normal group of a player.
     *
     * @param player The player.
     * @param group  The normal group.
     *
     * @return True on success, false on failure.
     */
    public boolean addNormal(Player player, String group) {
        // Check whether the permissions check is enabled
        if (!settings.getProperty(PluginSettings.ENABLE_PERMISSION_CHECK)) {
            return false;
        }

        // Remove old groups
        permissionsManager.removeGroups(player, Arrays.asList(Settings.unRegisteredGroup,
            Settings.getRegisteredGroup, Settings.getUnloggedinGroup));

        // Add the normal group, return the result
        return permissionsManager.addGroup(player, group);
    }

}
