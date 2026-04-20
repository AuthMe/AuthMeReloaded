package fr.xephi.authme.listener;

import fr.xephi.authme.service.TeleportationService;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import javax.inject.Inject;

/**
 * Shared async spawn-location listener shell for Paper-derived platforms.
 */
public abstract class AbstractPaperAsyncPlayerSpawnLocationListener implements Listener {

    @Inject
    protected TeleportationService teleportationService;

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerSpawn(AsyncPlayerSpawnLocationEvent event) {
        SpawnLocationTracker.markEventCalled();

        String playerName = event.getConnection().getProfile().getName();
        Location originalSpawnLocation = event.getSpawnLocation();

        Location customSpawnLocation = determineCustomSpawnLocation(playerName, originalSpawnLocation);
        if (customSpawnLocation != null) {
            event.setSpawnLocation(customSpawnLocation);
        }
    }

    protected abstract Location determineCustomSpawnLocation(String playerName, Location originalSpawnLocation);
}
