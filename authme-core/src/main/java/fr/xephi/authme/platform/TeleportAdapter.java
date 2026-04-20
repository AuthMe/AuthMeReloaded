package fr.xephi.authme.platform;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Platform-specific teleportation behavior.
 * May be asynchronous on platforms that support it (e.g. Paper).
 */
public interface TeleportAdapter {

    void teleportPlayer(Player player, Location location);
}
