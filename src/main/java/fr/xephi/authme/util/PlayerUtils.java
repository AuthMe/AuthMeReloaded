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
