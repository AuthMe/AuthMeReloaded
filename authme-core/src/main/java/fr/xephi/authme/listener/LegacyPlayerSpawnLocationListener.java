package fr.xephi.authme.listener;

import fr.xephi.authme.service.TeleportationService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import javax.inject.Inject;

/**
 * Handles the legacy Spigot spawn-location event on platforms that still use it.
 */
public class LegacyPlayerSpawnLocationListener implements Listener {

    @Inject
    private TeleportationService teleportationService;

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerSpawn(PlayerSpawnLocationEvent event) {
        SpawnLocationTracker.markEventCalled();
        final Player player = event.getPlayer();

        Location customSpawnLocation = teleportationService.prepareOnJoinSpawnLocation(player, event.getSpawnLocation());
        if (customSpawnLocation != null) {
            event.setSpawnLocation(customSpawnLocation);
        }
    }
}
