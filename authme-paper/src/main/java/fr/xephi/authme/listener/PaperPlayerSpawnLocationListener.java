package fr.xephi.authme.listener;

import fr.xephi.authme.service.BukkitService;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import org.bukkit.Location;

import javax.inject.Inject;

/**
 * Paper listener that resolves custom join spawn locations for async spawn events.
 */
public class PaperPlayerSpawnLocationListener extends AbstractPaperAsyncPlayerSpawnLocationListener {

    @Inject
    private BukkitService bukkitService;

    /**
     * Constructor.
     */
    public PaperPlayerSpawnLocationListener() {
    }

    @Override
    protected Location determineCustomSpawnLocation(String playerName, Location originalSpawnLocation) {
        return bukkitService.callSyncMethodFromOptionallyAsyncTask(
            () -> teleportationService.prepareOnJoinSpawnLocation(playerName, originalSpawnLocation));
    }
}
