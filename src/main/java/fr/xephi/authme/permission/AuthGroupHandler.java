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
     */
    public void setGroup(Player player, AuthGroupType groupType) {
        if (!useAuthGroups()) {
            return;
        }

        String primaryGroup = "";
        LimboPlayer limboPlayer = limboCache.getPlayerData(player.getName());
        if (limboPlayer != null) {
            primaryGroup = limboPlayer.getGroup();
        }

        switch (groupType) {
            // Implementation note: some permission systems don't support players not being in any group,
            // so add the new group before removing the old ones
            case UNREGISTERED:
                permissionsManager.addGroup(player, unregisteredGroup);
                permissionsManager.removeGroups(player, registeredGroup, primaryGroup);
                break;

            case REGISTERED_UNAUTHENTICATED:
                permissionsManager.addGroup(player, registeredGroup);
                permissionsManager.removeGroups(player, unregisteredGroup, primaryGroup);
                break;

            case LOGGED_IN:
                restoreGroup(player);
                break;

            default:
                throw new IllegalStateException("Encountered unhandled auth group type '" + groupType + "'");
        }

        ConsoleLogger.debug(
            () -> player.getName() + " changed to " + groupType + ": has groups " + permissionsManager.getGroups(player));
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

    /**
     * Restores the player's original primary group (taken from LimboPlayer).
     *
     * @param player the player to process
     */
    private void restoreGroup(Player player) {
        LimboPlayer limbo = limboCache.getPlayerData(player.getName());
        if (limbo != null) {
            String primaryGroup = limbo.getGroup();
            permissionsManager.addGroup(player, primaryGroup);
        }
        permissionsManager.removeGroups(player, unregisteredGroup, registeredGroup);
    }

    @Override
    @PostConstruct
    public void reload() {
        unregisteredGroup = settings.getProperty(PluginSettings.UNREGISTERED_GROUP);
        registeredGroup = settings.getProperty(PluginSettings.REGISTERED_GROUP);
    }

}
