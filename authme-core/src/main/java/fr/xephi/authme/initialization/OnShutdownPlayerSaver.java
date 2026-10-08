package fr.xephi.authme.initialization;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Locale;

/**
 * Saves all players' data when the plugin shuts down.
 */
public class OnShutdownPlayerSaver {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(OnShutdownPlayerSaver.class);

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
            try {
                savePlayer(player);
            } catch (RuntimeException e) {
                // One failing player (e.g. a region-owned read on Folia's shutdown thread) must not prevent the
                // limbo data of all the remaining players from being restored
                logger.logException("Could not save the data of '" + player.getName() + "' on shutdown:", e);
            }
        }
    }

    private void savePlayer(Player player) {
        String name = player.getName().toLowerCase(Locale.ROOT);
        if (PlayerUtils.isNpc(player) || validationService.isUnrestricted(name)) {
            return;
        }
        try {
            if (limboService.hasLimboPlayer(name)) {
                limboService.restoreData(player);
            } else {
                saveLoggedinPlayer(player);
            }
        } finally {
            playerCache.removePlayer(name);
        }
    }

    private void saveLoggedinPlayer(Player player) {
        if (settings.getProperty(RestrictionSettings.SAVE_QUIT_LOCATION)) {
            Location loc = spawnLoader.getPlayerLocationOrSpawn(player);
            PlayerAuth auth = PlayerAuth.builder()
                .name(player.getName().toLowerCase(Locale.ROOT))
                .realName(player.getName())
                .location(loc).build();
            dataSource.updateQuitLoc(auth);
            // TODO: send an update when a messaging service will be implemented (QUITLOC)
        }
    }
}
