package fr.xephi.authme.data.limbo;

import fr.xephi.authme.data.backup.LimboPlayerStorage;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages all {@link LimboPlayer} instances.
 */
public class LimboCache {

    private final Map<String, LimboPlayer> cache = new ConcurrentHashMap<>();

    private LimboPlayerStorage limboPlayerStorage;
    private Settings settings;
    private PermissionsManager permissionsManager;
    private SpawnLoader spawnLoader;

    @Inject
    LimboCache(Settings settings, PermissionsManager permissionsManager,
               SpawnLoader spawnLoader, LimboPlayerStorage limboPlayerStorage) {
        this.settings = settings;
        this.permissionsManager = permissionsManager;
        this.spawnLoader = spawnLoader;
        this.limboPlayerStorage = limboPlayerStorage;
    }

    /**
     * Load player data if exist, otherwise current player's data will be stored.
     *
     * @param player Player instance to add.
     */
    public void addPlayerData(Player player) {
        String name = player.getName().toLowerCase();
        Location location = spawnLoader.getPlayerLocationOrSpawn(player);
        boolean operator = player.isOp();
        boolean flyEnabled = player.getAllowFlight();
        float walkSpeed = player.getWalkSpeed();
        float flySpeed = player.getFlySpeed();
        String playerGroup = "";
        if (permissionsManager.hasGroupSupport()) {
            playerGroup = permissionsManager.getPrimaryGroup(player);
        }

        if (limboPlayerStorage.hasData(player)) {
            LimboPlayer cache = limboPlayerStorage.readData(player);
            if (cache != null) {
                location = cache.getLocation();
                playerGroup = cache.getGroup();
                operator = cache.isOperator();
                flyEnabled = cache.isCanFly();
                walkSpeed = cache.getWalkSpeed();
                flySpeed = cache.getFlySpeed();
            }
        } else {
            limboPlayerStorage.saveData(player);
        }

        cache.put(name, new LimboPlayer(location, operator, playerGroup, flyEnabled, walkSpeed, flySpeed));
    }

    /**
     * Restore player's data to player if exist.
     *
     * @param player Player instance to restore
     */
    public void restoreData(Player player) {
        String lowerName = player.getName().toLowerCase();
        if (cache.containsKey(lowerName)) {
            LimboPlayer data = cache.get(lowerName);
            player.setOp(data.isOperator());
            player.setAllowFlight(data.isCanFly());
            float walkSpeed = data.getWalkSpeed();
            float flySpeed = data.getFlySpeed();
            // Reset the speed value if it was 0
            if(walkSpeed == 0f) {
                walkSpeed = 0.2f;
            }
            if(flySpeed == 0f) {
                flySpeed = 0.2f;
            }
            player.setWalkSpeed(walkSpeed);
            player.setFlySpeed(flySpeed);
            restoreGroup(player, data.getGroup());
            data.clearTasks();
        }
    }

    /**
     * Remove PlayerData from cache and disk.
     *
     * @param player Player player to remove.
     */
    public void deletePlayerData(Player player) {
        removeFromCache(player);
        limboPlayerStorage.removeData(player);
    }

    /**
     * Remove PlayerData from cache.
     *
     * @param player player to remove.
     */
    public void removeFromCache(Player player) {
        String name = player.getName().toLowerCase();
        LimboPlayer cachedPlayer = cache.remove(name);
        if (cachedPlayer != null) {
            cachedPlayer.clearTasks();
        }
    }

    /**
     * Method getPlayerData.
     *
     * @param name String
     *
     * @return PlayerData
     */
    public LimboPlayer getPlayerData(String name) {
        checkNotNull(name);
        return cache.get(name.toLowerCase());
    }

    /**
     * Method hasPlayerData.
     *
     * @param name String
     *
     * @return boolean
     */
    public boolean hasPlayerData(String name) {
        checkNotNull(name);
        return cache.containsKey(name.toLowerCase());
    }

    /**
     * Method updatePlayerData.
     *
     * @param player Player
     */
    public void updatePlayerData(Player player) {
        checkNotNull(player);
        removeFromCache(player);
        addPlayerData(player);
    }

    private void restoreGroup(Player player, String group) {
        if (!StringUtils.isEmpty(group) && permissionsManager.hasGroupSupport()
            && settings.getProperty(PluginSettings.ENABLE_PERMISSION_CHECK)) {
            permissionsManager.setGroup(player, group);
        }
    }
}
