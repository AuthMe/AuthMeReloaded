package fr.xephi.authme.listener;

import fr.xephi.authme.service.TeleportationService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import javax.inject.Inject;

public class PlayerListener19Spigot implements Listener {

    private static boolean isPlayerSpawnLocationEventCalled = false;

    @Inject
    private TeleportationService teleportationService;


    public static boolean isPlayerSpawnLocationEventCalled() {
        return isPlayerSpawnLocationEventCalled;
    }

    // Note: the following event is called since MC1.9, in older versions we have to fallback on the PlayerJoinEvent
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerSpawn(PlayerSpawnLocationEvent event) {
        isPlayerSpawnLocationEventCalled = true;
        final Player player = event.getPlayer();

        Location customSpawnLocation = teleportationService.prepareOnJoinSpawnLocation(player);
        if (customSpawnLocation != null) {
            event.setSpawnLocation(customSpawnLocation);
        }
    }

}
