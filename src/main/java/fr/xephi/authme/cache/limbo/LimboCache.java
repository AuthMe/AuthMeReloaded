package fr.xephi.authme.cache.limbo;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.backup.DataFileCache;
import fr.xephi.authme.cache.backup.JsonCache;
import fr.xephi.authme.events.ResetInventoryEvent;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;

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
        GameMode gameMode = player.getGameMode();
        boolean operator = false;
        String playerGroup = "";
        boolean flying = false;

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
                flying = cache.isFlying();
            }
        } else {
            operator = player.isOp();
            flying = player.isFlying();

            // Check whether groups are supported
            if (permsMan.hasGroupSupport())
                playerGroup = permsMan.getPrimaryGroup(player);
        }

        if (Settings.isForceSurvivalModeEnabled) {
            if (Settings.isResetInventoryIfCreative && gameMode == GameMode.CREATIVE) {
                ResetInventoryEvent event = new ResetInventoryEvent(player);
                Bukkit.getServer().getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    player.getInventory().clear();
                    player.sendMessage("Your inventory has been cleaned!");
                }
            }
            if (gameMode == GameMode.CREATIVE) {
                flying = false;
            }
            gameMode = GameMode.SURVIVAL;
        }
        if (player.isDead()) {
            loc = plugin.getSpawnLocation(player);
        }
        cache.put(name, new LimboPlayer(name, loc, gameMode, operator, playerGroup, flying));
    }

    /**
     * Method addLimboPlayer.
     *
     * @param player Player
     * @param group  String
     */
    public void addLimboPlayer(Player player, String group) {
        cache.put(player.getName().toLowerCase(), new LimboPlayer(player.getName().toLowerCase(), group));
    }

    /**
     * Method deleteLimboPlayer.
     *
     * @param name String
     */
    public void deleteLimboPlayer(String name) {
    	if (name == null)
    		return;
        cache.remove(name);
    }

    /**
     * Method getLimboPlayer.
     *
     * @param name String
     *
     * @return LimboPlayer
     */
    public LimboPlayer getLimboPlayer(String name) {
    	if (name == null)
    		return null;
        return cache.get(name);
    }

    /**
     * Method hasLimboPlayer.
     *
     * @param name String
     *
     * @return boolean
     */
    public boolean hasLimboPlayer(String name) {
    	if (name == null)
    		return false;
        return cache.containsKey(name);
    }

    /**
     * Method updateLimboPlayer.
     *
     * @param player Player
     */
    public void updateLimboPlayer(Player player) {
        if (this.hasLimboPlayer(player.getName().toLowerCase())) {
            this.deleteLimboPlayer(player.getName().toLowerCase());
        }
        addLimboPlayer(player);
    }

}
