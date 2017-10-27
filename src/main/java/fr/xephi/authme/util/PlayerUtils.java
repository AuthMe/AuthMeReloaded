package fr.xephi.authme.util;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Player utilities.
 */
public final class PlayerUtils {

    // Utility class
    private PlayerUtils() {
    }

    /**
     * Get player's UUID if can, name otherwise.
     *
     * @param player Player to retrieve
     *
     * @return player's UUID or Name in String.
     */
    public static String getUuidOrName(OfflinePlayer player) {
        // We may made this configurable in future
        // so we can have uuid support.
        try {
            return player.getUniqueId().toString();
        } catch (NoSuchMethodError ignore) {
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

    /**
     * Returns if the player is an NPC or not.
     *
     * @param player The player to check
     *
     * @return True if the player is an NPC, false otherwise
     */
    public static boolean isNpc(Player player) {
       return player.hasMetadata("NPC");
    }
}
