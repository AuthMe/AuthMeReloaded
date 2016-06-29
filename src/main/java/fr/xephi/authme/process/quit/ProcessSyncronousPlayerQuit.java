package fr.xephi.authme.process.quit;

import fr.xephi.authme.cache.backup.JsonCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.process.SynchronousProcess;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.StringUtils;
import fr.xephi.authme.util.Utils;
import org.bukkit.entity.Player;

import javax.inject.Inject;


public class ProcessSyncronousPlayerQuit implements SynchronousProcess {

    @Inject
    private JsonCache jsonCache;

    @Inject
    private ProcessService service;

    @Inject
    private LimboCache limboCache;

    public void processSyncQuit(Player player) {
        LimboPlayer limbo = limboCache.getLimboPlayer(player.getName().toLowerCase());
        if (limbo != null) { // it mean player is not authenticated
            // Only delete if we don't need player's last location
            if (service.getProperty(RestrictionSettings.TELEPORT_UNAUTHED_TO_SPAWN)) {
                limboCache.removeLimboPlayer(player);
            } else {
                // Restore data if its about to delete LimboPlayer
                if (!StringUtils.isEmpty(limbo.getGroup())) {
                    Utils.addNormal(player, limbo.getGroup());
                }
                player.setOp(limbo.isOperator());
                player.setAllowFlight(limbo.isCanFly());
                player.setWalkSpeed(limbo.getWalkSpeed());
                limboCache.deleteLimboPlayer(player);
            }
        } else {
            // Write player's location, so we could retrieve it later on player next join
            if (service.getProperty(RestrictionSettings.TELEPORT_UNAUTHED_TO_SPAWN)) {
                if (!jsonCache.doesCacheExist(player)) {
                    jsonCache.writeCache(player);
                }
            }
        }

        player.leaveVehicle();
    }
}
