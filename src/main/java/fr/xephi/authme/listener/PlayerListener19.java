package fr.xephi.authme.listener;

import javax.inject.Inject;

import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.TeleportationService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

/**
 * Listener of player events for events introduced in Minecraft 1.9.
 */
public class PlayerListener19 implements Listener {

    @Inject
    private Management management;

    @Inject
    private TeleportationService teleportationService;

    @Inject
    private ListenerService listenerService;

    private static boolean IS_PLAYER_SPAWN_LOCATION_EVENT_CALLED = false;

    public static boolean isIsPlayerSpawnLocationEventCalled() {
        return IS_PLAYER_SPAWN_LOCATION_EVENT_CALLED;
    }

    // Note: the following event is called since MC1.9, in older versions we have to fallback on the PlayerJoinEvent
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerSpawn(PlayerSpawnLocationEvent event) {
        PlayerListener19.IS_PLAYER_SPAWN_LOCATION_EVENT_CALLED = true;
        final Player player = event.getPlayer();

        management.performJoin(player, event.getSpawnLocation());

        Location customSpawnLocation = teleportationService.prepareOnJoinSpawnLocation(player);
        if (customSpawnLocation != null) {
            event.setSpawnLocation(customSpawnLocation);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

}
