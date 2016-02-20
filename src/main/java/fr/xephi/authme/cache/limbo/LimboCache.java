package fr.xephi.authme.cache.limbo;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.backup.DataFileCache;
import fr.xephi.authme.cache.backup.JsonCache;
import fr.xephi.authme.permission.PermissionsManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 */
public class LimboCache {

    private volatile static LimboCache singleton;
    public final ConcurrentHashMap<String, LimboPlayer> cache;
    public final AuthMe plugin;
    private final JsonCache playerData;

    /**
     * Constructor for LimboCache.
     *
     * @param plugin AuthMe
     */
    private LimboCache(AuthMe plugin) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
        this.playerData = new JsonCache();
    }

    /**
     * Method getInstance.
     *
     * @return LimboCache
     */
    public static LimboCache getInstance() {
        if (singleton == null) {
            singleton = new LimboCache(AuthMe.getInstance());
        }
        return singleton;
    }

    /**
     * Add a limbo player.
     *
     * @param player Player instance to add.
     */
    public void addLimboPlayer(Player player) {
        String name = player.getName().toLowerCase();
        Location loc = player.getLocation();
        boolean operator = false;
        boolean flyEnabled = false;
        String playerGroup = "";

        // Get the permissions manager, and make sure it's valid
        PermissionsManager permsMan = this.plugin.getPermissionsManager();
        if (permsMan == null)
            ConsoleLogger.showError("Unable to access permissions manager!");
        assert permsMan != null;

        if (playerData.doesCacheExist(player)) {
            DataFileCache cache = playerData.readCache(player);
            if (cache != null) {
                playerGroup = cache.getGroup();
                operator = cache.getOperator();
                flyEnabled = cache.isFlyEnabled();
            }
        } else {
            operator = player.isOp();

            // Check whether groups are supported
            if (permsMan.hasGroupSupport()) {
                playerGroup = permsMan.getPrimaryGroup(player);
            }
        }

        if (player.isDead()) {
            loc = plugin.getSpawnLocation(player);
        }

        cache.put(name, new LimboPlayer(name, loc, operator, playerGroup, flyEnabled));
    }

    /**
     * Method deleteLimboPlayer.
     *
     * @param name String
     */
    public void deleteLimboPlayer(String name) {
        checkNotNull(name);
        cache.remove(name.toLowerCase());
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
        deleteLimboPlayer(player.getName().toLowerCase());
        addLimboPlayer(player);
    }

}
