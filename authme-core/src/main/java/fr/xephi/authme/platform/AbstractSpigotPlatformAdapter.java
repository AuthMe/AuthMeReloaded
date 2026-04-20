package fr.xephi.authme.platform;

import fr.xephi.authme.util.Utils;
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

    protected final String getCompatibilityError(String errorMessage, String... requiredClasses) {
        for (String className : requiredClasses) {
            if (!Utils.isClassLoaded(className)) {
                return errorMessage;
            }
        }
        return null;
    }
}
