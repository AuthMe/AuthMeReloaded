package fr.xephi.authme.util;

import fr.xephi.authme.ConsoleLogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.regex.Pattern;

/**
 * Utility class for various operations used in the codebase.
 */
public final class Utils {

    /** Number of milliseconds in a minute. */
    public static final long MILLIS_PER_MINUTE = 60_000L;
    /** Number of milliseconds in an hour. */
    public static final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;

    private Utils() {
    }

    /**
     * Get player's UUID if can, name otherwise.
     *
     * @param player Player to retrieve
     *
     * @return player's UUID or Name in String.
     */
    public static String getUUIDorName(OfflinePlayer player) {
        // We may made this configurable in future
        // so we can have uuid support.
        try {
            return player.getUniqueId().toString();
        } catch (NoSuchMethodError ignore) {
            return player.getName();
        }
    }

    /**
     * Compile Pattern sneaky without throwing Exception.
     *
     * @param pattern pattern string to compile
     *
     * @return the given regex compiled into Pattern object.
     */
    public static Pattern safePatternCompile(String pattern) {
        try {
            return Pattern.compile(pattern);
        } catch (Exception e) {
            ConsoleLogger.warning("Failed to compile pattern '" + pattern + "' - defaulting to allowing everything");
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

    /**
     * Returns whether the class exists in the current class loader.
     *
     * @param className the class name to check
     *
     * @return true if the class is loaded, false otherwise
     */
    public static boolean isClassLoaded(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
