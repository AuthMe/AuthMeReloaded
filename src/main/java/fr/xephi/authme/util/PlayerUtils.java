package fr.xephi.authme.util;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

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
     * @param player The player to return the IP address for
     *
     * @return The player's IP address
     */
    @NotNull
    public static String getPlayerIp(@NotNull Player player) {
        Objects.requireNonNull(player.getAddress(), "address");
        return player.getAddress().getAddress().getHostAddress();
    }

    /**
     * Returns if the player is an NPC or not.
     *
     * @param player The player to check
     *
     * @return True if the player is an NPC, false otherwise
     */
    public static boolean isNpc(@NotNull Player player) {
        return player.hasMetadata("NPC");
    }

}
