package fr.xephi.authme.util;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.events.AuthMeTeleportEvent;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * Utility class for various operations used in the codebase.
 */
public final class Utils {

    private static AuthMe plugin = AuthMe.getInstance();

    private Utils() {
    }

    /**
     * Set the group of a player, by its AuthMe group type.
     *
     * @param player The player.
     * @param group  The group type.
     *
     * @return True if succeeded, false otherwise. False is also returned if groups aren't supported
     * with the current permissions system.
     */
    public static boolean setGroup(Player player, GroupType group) {
        // Check whether the permissions check is enabled
        if (!Settings.isPermissionCheckEnabled) {
            return false;
        }

        // Get the permissions manager, and make sure it's valid
        PermissionsManager permsMan = plugin.getPermissionsManager();
        if (permsMan == null) {
            ConsoleLogger.showError("Failed to access permissions manager instance, shutting down.");
            return false;
        }

        // Make sure group support is available
        if (!permsMan.hasGroupSupport()) {
            ConsoleLogger.showError("The current permissions system doesn't have group support, unable to set group!");
            return false;
        }

        switch (group) {
            case UNREGISTERED:
                // Remove the other group type groups, set the current group
                permsMan.removeGroups(player, Arrays.asList(Settings.getRegisteredGroup, Settings.getUnloggedinGroup));
                return permsMan.addGroup(player, Settings.unRegisteredGroup);

            case REGISTERED:
                // Remove the other group type groups, set the current group
                permsMan.removeGroups(player, Arrays.asList(Settings.unRegisteredGroup, Settings.getUnloggedinGroup));
                return permsMan.addGroup(player, Settings.getRegisteredGroup);

            case NOTLOGGEDIN:
                // Remove the other group type groups, set the current group
                permsMan.removeGroups(player, Arrays.asList(Settings.unRegisteredGroup, Settings.getRegisteredGroup));
                return permsMan.addGroup(player, Settings.getUnloggedinGroup);

            case LOGGEDIN:
                // Get the limbo player data
                LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(player.getName().toLowerCase());
                if (limbo == null)
                    return false;

                // Get the players group
                String realGroup = limbo.getGroup();

                // Remove the other group types groups, set the real group
                permsMan.removeGroups(player, Arrays.asList(Settings.unRegisteredGroup, Settings.getRegisteredGroup, Settings.getUnloggedinGroup));
                return permsMan.addGroup(player, realGroup);

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
    public static boolean addNormal(Player player, String group) {
        if (!Settings.isPermissionCheckEnabled) {
            return false;
        }

        // Get the permissions manager, and make sure it's valid
        PermissionsManager permsMan = plugin.getPermissionsManager();
        if (permsMan == null) {
            ConsoleLogger.showError("Failed to access permissions manager instance, aborting.");
            return false;
        }

        // Remove old groups
        permsMan.removeGroups(player, Arrays.asList(Settings.unRegisteredGroup,
            Settings.getRegisteredGroup, Settings.getUnloggedinGroup));

        // Add the normal group, return the result
        return permsMan.addGroup(player, group);
    }

    @Deprecated
    public static boolean isUnrestricted(Player player) {
        // TODO ljacqu 20160602: Checking for Settings.isAllowRestrictedIp is wrong! Nothing in the config suggests
        // that this setting has anything to do with unrestricted names
        return Settings.isAllowRestrictedIp
            && Settings.getUnrestrictedName.contains(player.getName().toLowerCase());
    }

    @Deprecated
    public static void teleportToSpawn(Player player) {
        if (Settings.isTeleportToSpawnEnabled && !Settings.noTeleport) {
            Location spawn = plugin.getSpawnLocation(player);
            AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(player, spawn);
            plugin.getServer().getPluginManager().callEvent(tpEvent);
            if (!tpEvent.isCancelled()) {
                player.teleport(tpEvent.getTo());
            }
        }
    }

    public static String getUUIDorName(OfflinePlayer player) {
        try {
            return player.getUniqueId().toString();
        } catch (Exception ignore) {
            return player.getName();
        }
    }

    public enum GroupType {
        UNREGISTERED,
        REGISTERED,
        NOTLOGGEDIN,
        LOGGEDIN
    }

    /**
     * Returns the IP of the given player.
     *
     * @param p The player to return the IP address for
     *
     * @return The player's IP address
     */
    public static String getPlayerIp(Player p) {
        return p.getAddress().getAddress().getHostAddress();
    }
}
