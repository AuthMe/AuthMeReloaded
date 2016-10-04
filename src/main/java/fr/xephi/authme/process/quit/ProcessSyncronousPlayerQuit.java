package fr.xephi.authme.process.quit;

import fr.xephi.authme.data.backup.LimboPlayerStorage;
import fr.xephi.authme.data.limbo.LimboCache;
import fr.xephi.authme.process.SynchronousProcess;
import org.bukkit.entity.Player;

import javax.inject.Inject;


public class ProcessSyncronousPlayerQuit implements SynchronousProcess {

    @Inject
    private LimboPlayerStorage limboPlayerStorage;

    @Inject
    private LimboCache limboCache;

    public void processSyncQuit(Player player) {
        if (limboCache.hasPlayerData(player.getName())) { // it mean player is not authenticated
            limboCache.restoreData(player);
            limboCache.removeFromCache(player);
        } else {
            // Save player's data, so we could retrieve it later on player next join
            if (!limboPlayerStorage.hasData(player)) {
                limboPlayerStorage.saveData(player);
            }
        }

        player.leaveVehicle();
    }
}
