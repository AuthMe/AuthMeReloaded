package fr.xephi.authme.process.quit;

import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.process.SynchronousProcess;
import org.bukkit.entity.Player;

import javax.inject.Inject;


public class ProcessSyncronousPlayerQuit implements SynchronousProcess {

    @Inject
    private LimboService limboService;

    public void processSyncQuit(Player player) {
        limboService.restoreData(player);
        player.leaveVehicle();
    }
}
