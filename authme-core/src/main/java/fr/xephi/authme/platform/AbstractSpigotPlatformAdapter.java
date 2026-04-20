package fr.xephi.authme.platform;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Base implementation of {@link PlatformAdapter} for all Spigot versions.
 * Uses synchronous (blocking) teleport via the Bukkit API.
 */
public abstract class AbstractSpigotPlatformAdapter implements PlatformAdapter {

    @Override
    public void teleportPlayer(Player player, Location location) {
        player.teleport(location);
    }
}
