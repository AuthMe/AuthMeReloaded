package fr.xephi.authme.process.quit;

import fr.xephi.authme.cache.backup.PlayerDataStorage;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.permission.AuthGroupHandler;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.process.SynchronousProcess;
import org.bukkit.entity.Player;

import javax.inject.Inject;


public class ProcessSyncronousPlayerQuit implements SynchronousProcess {

    @Inject
    private PlayerDataStorage playerDataStorage;

    @Inject
    private ProcessService service;

    @Inject
    private LimboCache limboCache;

    @Inject
    private AuthGroupHandler authGroupHandler;

    public void processSyncQuit(Player player) {
        if (limboCache.hasPlayerData(player.getName().toLowerCase())) { // it mean player is not authenticated
            limboCache.removeFromCache(player);
        } else {
            // Save player's data, so we could retrieve it later on player next join
            if (!playerDataStorage.hasData(player)) {
                playerDataStorage.saveData(player);
            }
        }

        player.leaveVehicle();
    }
}
