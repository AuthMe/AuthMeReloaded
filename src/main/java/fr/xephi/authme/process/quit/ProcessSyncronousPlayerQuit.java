package fr.xephi.authme.process.quit;

import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.process.SynchronousProcess;
import fr.xephi.authme.util.StringUtils;
import fr.xephi.authme.util.Utils;
import org.bukkit.entity.Player;

import javax.inject.Inject;


public class ProcessSyncronousPlayerQuit implements SynchronousProcess {

    @Inject
    private LimboCache limboCache;

    public void processSyncQuit(Player player) {
        LimboPlayer limbo = limboCache.getLimboPlayer(player.getName().toLowerCase());
        if (limbo != null) {
            if (!StringUtils.isEmpty(limbo.getGroup())) {
                Utils.addNormal(player, limbo.getGroup());
            }
            player.setOp(limbo.isOperator());
            player.setAllowFlight(limbo.isCanFly());
            player.setWalkSpeed(limbo.getWalkSpeed());
            limboCache.deleteLimboPlayer(player);
        }
        player.leaveVehicle();
    }
}
