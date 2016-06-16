package fr.xephi.authme.process.quit;

import fr.xephi.authme.process.SynchronousProcess;
import org.bukkit.entity.Player;


public class ProcessSyncronousPlayerQuit implements SynchronousProcess {

    public void processSyncQuit(Player player, boolean isOp, boolean needToChange) {
        if (needToChange) {
            player.setOp(isOp);
        }
        player.leaveVehicle();
    }
}
