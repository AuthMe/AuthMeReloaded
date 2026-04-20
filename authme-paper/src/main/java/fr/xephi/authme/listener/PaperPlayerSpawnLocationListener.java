package fr.xephi.authme.listener;

import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.TeleportationService;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import javax.inject.Inject;

public class PaperPlayerSpawnLocationListener implements Listener {

    @Inject
    private BukkitService bukkitService;

    @Inject
    private TeleportationService teleportationService;

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerSpawn(AsyncPlayerSpawnLocationEvent event) {
        SpawnLocationTracker.markEventCalled();

        final String playerName = event.getConnection().getProfile().getName();
        final Location originalSpawnLocation = event.getSpawnLocation();
        final World spawnWorld = originalSpawnLocation == null ? null : originalSpawnLocation.getWorld();

        Location customSpawnLocation = bukkitService.callSyncMethodFromOptionallyAsyncTask(
            () -> teleportationService.prepareOnJoinSpawnLocation(playerName, spawnWorld));
        if (customSpawnLocation != null) {
            event.setSpawnLocation(customSpawnLocation);
        }
    }
}
