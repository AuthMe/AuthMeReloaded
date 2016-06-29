package fr.xephi.authme.cache.limbo;

import fr.xephi.authme.cache.backup.JsonCache;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.SpawnLoader;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages all {@link LimboPlayer} instances.
 */
public class LimboCache {

    private final ConcurrentHashMap<String, LimboPlayer> cache = new ConcurrentHashMap<>();

    private JsonCache jsonCache;
    private PermissionsManager permissionsManager;
    private SpawnLoader spawnLoader;

    @Inject
    LimboCache(PermissionsManager permissionsManager, SpawnLoader spawnLoader, JsonCache jsonCache) {
        this.permissionsManager = permissionsManager;
        this.spawnLoader = spawnLoader;
        this.jsonCache = jsonCache;
    }

    /**
     * Add a limbo player.
     *
     * @param player Player instance to add.
     */
    public void addLimboPlayer(Player player) {
        String name = player.getName().toLowerCase();
        Location location = player.isDead() ? spawnLoader.getSpawnLocation(player) : player.getLocation();
        boolean operator = player.isOp();
        boolean flyEnabled = player.getAllowFlight();
        float walkSpeed = player.getWalkSpeed();
        String playerGroup = "";
        if (permissionsManager.hasGroupSupport()) {
            playerGroup = permissionsManager.getPrimaryGroup(player);
        }

        if (jsonCache.doesCacheExist(player)) {
            LimboPlayer cache = jsonCache.readCache(player);
            if (cache != null) {
                location = cache.getLoc();
                playerGroup = cache.getGroup();
                operator = cache.isOperator();
                flyEnabled = cache.isCanFly();
                walkSpeed = cache.getWalkSpeed();
            }
        } else {
            jsonCache.writeCache(player);
        }

        cache.put(name, new LimboPlayer(name, location, operator, playerGroup, flyEnabled, walkSpeed));
    }

    /**
     * Remove LimboPlayer and delete cache.json from disk.
     *
     * @param player Player player to remove.
     */
    public void deleteLimboPlayer(Player player) {
        removeLimboPlayer(player);
        jsonCache.removeCache(player);
    }

    /**
     * Remove LimboPlayer from cache, without deleting cache.json file.
     *
     * @param player Player player to remove.
     */
    public void removeLimboPlayer(Player player) {
        String name = player.getName().toLowerCase();
        LimboPlayer cachedPlayer = cache.remove(name);
        if (cachedPlayer != null) {
            cachedPlayer.clearTasks();
        }
    }

    /**
     * Method getLimboPlayer.
     *
     * @param name String
     *
     * @return LimboPlayer
     */
    public LimboPlayer getLimboPlayer(String name) {
        checkNotNull(name);
        return cache.get(name.toLowerCase());
    }

    /**
     * Method hasLimboPlayer.
     *
     * @param name String
     *
     * @return boolean
     */
    public boolean hasLimboPlayer(String name) {
        checkNotNull(name);
        return cache.containsKey(name.toLowerCase());
    }

    /**
     * Method updateLimboPlayer.
     *
     * @param player Player
     */
    public void updateLimboPlayer(Player player) {
        checkNotNull(player);
        removeLimboPlayer(player);
        addLimboPlayer(player);
    }
}
