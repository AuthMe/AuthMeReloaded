package fr.xephi.authme.platform;

import fr.xephi.authme.listener.PlayerListener19Spigot;
import org.bukkit.event.Listener;

import java.util.Collections;
import java.util.List;

/**
 * Platform adapter for Spigot 1.16–1.19 (legacy versions).
 */
public class SpigotPlatformAdapter extends AbstractSpigotPlatformAdapter {

    @Override
    public String getPlatformName() {
        return "spigot-legacy";
    }

    @Override
    public String getCompatibilityError() {
        return getCompatibilityError("This AuthMe Spigot Legacy build requires the Spigot 1.16+ API.",
            "org.spigotmc.event.player.PlayerSpawnLocationEvent");
    }

    @Override
    public List<Class<? extends Listener>> getAdditionalListeners() {
        return Collections.singletonList(PlayerListener19Spigot.class);
    }
}
