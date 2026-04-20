package fr.xephi.authme.listener;

import org.bukkit.Location;
public class FoliaPlayerSpawnLocationListener extends AbstractPaperAsyncPlayerSpawnLocationListener {

    @Override
    protected Location determineCustomSpawnLocation(String playerName, Location originalSpawnLocation) {
        return teleportationService.prepareOnJoinSpawnLocation(playerName, originalSpawnLocation);
    }
}
