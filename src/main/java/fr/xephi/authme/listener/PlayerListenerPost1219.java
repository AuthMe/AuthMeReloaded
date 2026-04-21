package fr.xephi.authme.listener;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.TeleportationService;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import javax.inject.Inject;
import java.util.concurrent.ExecutionException;

public class PlayerListenerPost1219 implements Listener {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(PlayerListenerPost1219.class);

    @Inject
    private TeleportationService teleportationService;
    @Inject
    private BukkitService bukkitService;

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPlayerSpawnLocation(AsyncPlayerSpawnLocationEvent event) {
        var connection = event.getConnection();
        var profile = connection.getProfile();

        try {
            var customSpawnLocation = bukkitService.callSyncMethod(
                () -> teleportationService.prepareOnJoinSpawnLocation(profile, event.getSpawnLocation())
            ).get();
            event.setSpawnLocation(customSpawnLocation);
        } catch (InterruptedException | ExecutionException e) {
            logger.logException("Error while preparing spawn location", e);
        }
    }
}
