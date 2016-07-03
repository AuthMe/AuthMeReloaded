package fr.xephi.authme.cache.limbo;

import fr.xephi.authme.cache.backup.PlayerDataStorage;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.SpawnLoader;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages all {@link PlayerData} instances.
 */
public class LimboCache {

    private final ConcurrentHashMap<String, PlayerData> cache = new ConcurrentHashMap<>();

    private PlayerDataStorage playerDataStorage;
    private PermissionsManager permissionsManager;
    private SpawnLoader spawnLoader;

    @Inject
    LimboCache(PermissionsManager permissionsManager, SpawnLoader spawnLoader, PlayerDataStorage playerDataStorage) {
        this.permissionsManager = permissionsManager;
        this.spawnLoader = spawnLoader;
        this.playerDataStorage = playerDataStorage;
    }

    /**
     * Add a limbo player.
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

        if (playerDataStorage.hasData(player)) {
            PlayerData cache = playerDataStorage.readData(player);
            if (cache != null) {
                location = cache.getLoc();
                playerGroup = cache.getGroup();
                operator = cache.isOperator();
                flyEnabled = cache.isCanFly();
                walkSpeed = cache.getWalkSpeed();
                flySpeed = cache.getFlySpeed();
            }
        } else {
            playerDataStorage.saveData(player);
        }

        cache.put(name, new PlayerData(location, operator, playerGroup, flyEnabled, walkSpeed, flySpeed));
    }

    /**
     * Remove PlayerData and delete cache.json from disk.
     *
     * @param player Player player to remove.
     */
    public void deletePlayerData(Player player) {
        removePlayerData(player);
        playerDataStorage.removeData(player);
    }

    /**
     * Remove PlayerData from cache, without deleting cache.json file.
     *
     * @param player Player player to remove.
     */
    public void removePlayerData(Player player) {
        String name = player.getName().toLowerCase();
        PlayerData cachedPlayer = cache.remove(name);
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
    public PlayerData getPlayerData(String name) {
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
        removePlayerData(player);
        addPlayerData(player);
    }
}
