package fr.xephi.authme.platform;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Platform-specific teleportation behavior.
 * May be asynchronous on platforms that support it (e.g. Paper).
 */
public interface TeleportAdapter {

    void teleportPlayer(Player player, Location location);

    /**
     * Returns the platform-appropriate respawn location for the player, or null if there is no valid respawn point.
     * <p>
     * Legacy platforms only expose bed spawn directly, while newer APIs can provide the server's full respawn target
     * (bed, respawn anchor, world spawn logic, etc.).
     *
     * @param player the player
     * @return the respawn location, or null if none is available
     */
    Location getPlayerRespawnLocation(Player player);
}
