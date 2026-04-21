package fr.xephi.authme.listener;

import fr.xephi.authme.service.TeleportationService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import javax.inject.Inject;

@SuppressWarnings("removal")
public class PlayerListenerPre1219 implements Listener {

    @Inject
    private TeleportationService teleportationService;

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerSpawn(org.spigotmc.event.player.PlayerSpawnLocationEvent event) {
        var player = event.getPlayer();
        var profile = player.getPlayerProfile();

        var customSpawnLocation = teleportationService.prepareOnJoinSpawnLocation(profile, event.getSpawnLocation());
        if (customSpawnLocation != null) {
            event.setSpawnLocation(customSpawnLocation);
        }
    }
}
