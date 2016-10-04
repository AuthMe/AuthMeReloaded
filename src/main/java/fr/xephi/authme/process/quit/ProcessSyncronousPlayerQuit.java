package fr.xephi.authme.process.quit;

import fr.xephi.authme.data.backup.PlayerDataStorage;
import fr.xephi.authme.data.limbo.LimboStorage;
import fr.xephi.authme.process.SynchronousProcess;
import org.bukkit.entity.Player;

import javax.inject.Inject;


public class ProcessSyncronousPlayerQuit implements SynchronousProcess {

    @Inject
    private PlayerDataStorage playerDataStorage;

    @Inject
    private LimboStorage limboStorage;

    public void processSyncQuit(Player player) {
        if (limboStorage.hasPlayerData(player.getName())) { // it mean player is not authenticated
            limboStorage.restoreData(player);
            limboStorage.removeFromCache(player);
        } else {
            // Save player's data, so we could retrieve it later on player next join
            if (!playerDataStorage.hasData(player)) {
                playerDataStorage.saveData(player);
            }
        }

        player.leaveVehicle();
    }
}
