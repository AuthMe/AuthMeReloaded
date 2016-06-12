package fr.xephi.authme.util;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
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
