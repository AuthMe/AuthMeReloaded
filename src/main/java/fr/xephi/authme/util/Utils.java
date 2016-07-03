package fr.xephi.authme.util;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.Settings;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.regex.Pattern;

/**
 * Utility class for various operations used in the codebase.
 */
public final class Utils {

    private Utils() {
    }

    @Deprecated
    public static boolean isUnrestricted(Player player) {
        // TODO ljacqu 20160602: Checking for Settings.isAllowRestrictedIp is wrong! Nothing in the config suggests
        // that this setting has anything to do with unrestricted names
        return Settings.isAllowRestrictedIp
            && Settings.getUnrestrictedName.contains(player.getName().toLowerCase());
    }

    public static String getUUIDorName(OfflinePlayer player) {
        try {
            return player.getUniqueId().toString();
        } catch (Exception ignore) {
            return player.getName();
        }
    }

    public static Pattern safePatternCompile(String pattern) {
        try {
            return Pattern.compile(pattern);
        } catch (Exception e) {
            ConsoleLogger.showError("Failed to compile pattern '" + pattern + "' - defaulting to allowing everything");
            return Pattern.compile(".*?");
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
