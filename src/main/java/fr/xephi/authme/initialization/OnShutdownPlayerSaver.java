package fr.xephi.authme.initialization;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.backup.LimboPlayerStorage;
import fr.xephi.authme.data.limbo.LimboCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.service.PluginHookService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.ValidationService;
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
    private LimboCache limboCache;
    @Inject
    private DataSource dataSource;
    @Inject
    private LimboPlayerStorage limboPlayerStorage;
    @Inject
    private SpawnLoader spawnLoader;
    @Inject
    private PluginHookService pluginHookService;
    @Inject
    private PlayerCache playerCache;

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
        final String name = player.getName().toLowerCase();
        if (pluginHookService.isNpc(player) || validationService.isUnrestricted(name)) {
            return;
        }
        if (limboCache.hasPlayerData(name)) {
            limboCache.restoreData(player);
            limboCache.removeFromCache(player);
        } else {
            saveLoggedinPlayer(player);
        }
        playerCache.removePlayer(name);
    }

    private void saveLoggedinPlayer(Player player) {
        if (settings.getProperty(RestrictionSettings.SAVE_QUIT_LOCATION)) {
            Location loc = spawnLoader.getPlayerLocationOrSpawn(player);
            final PlayerAuth auth = PlayerAuth.builder()
                .name(player.getName().toLowerCase())
                .realName(player.getName())
                .location(loc).build();
            dataSource.updateQuitLoc(auth);
        }
        if (settings.getProperty(RestrictionSettings.TELEPORT_UNAUTHED_TO_SPAWN)
            && !settings.getProperty(RestrictionSettings.NO_TELEPORT)) {
            if (!limboPlayerStorage.hasData(player)) {
                limboPlayerStorage.saveData(player);
            }
        }
    }
}
