package fr.xephi.authme.initialization;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.inject.Inject;

/**
 * Saves all players' data when the plugin shuts down.
 */
public class OnShutdownPlayerSaver {

    @Inject
    private BukkitService bukkitService;
    @Inject
    private Settings settings;
    @Inject
    private ValidationService validationService;
    @Inject
    private DataSource dataSource;
    @Inject
    private SpawnLoader spawnLoader;
    @Inject
    private PlayerCache playerCache;
    @Inject
    private LimboService limboService;

    OnShutdownPlayerSaver() {
    }

    /**
     * Saves the data of all online players.
     */
    public void saveAllPlayers() {
        for (Player player : bukkitService.getOnlinePlayers()) {
            savePlayer(player);
        }
    }

    private void savePlayer(Player player) {
        String name = player.getName().toLowerCase();
        if (PlayerUtils.isNpc(player) || validationService.isUnrestricted(name)) {
            return;
        }
        if (limboService.hasLimboPlayer(name)) {
            limboService.restoreData(player);
        } else {
            saveLoggedinPlayer(player);
        }
        playerCache.removePlayer(name);
    }

    private void saveLoggedinPlayer(Player player) {
        if (settings.getProperty(RestrictionSettings.SAVE_QUIT_LOCATION)) {
            Location loc = spawnLoader.getPlayerLocationOrSpawn(player);
            PlayerAuth auth = PlayerAuth.builder()
                .name(player.getName().toLowerCase())
                .realName(player.getName())
                .location(loc).build();
            dataSource.updateQuitLoc(auth);
            // TODO: send an update when a messaging service will be implemented (QUITLOC)
        }
    }
}
