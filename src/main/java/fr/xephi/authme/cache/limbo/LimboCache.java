package fr.xephi.authme.cache.limbo;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.backup.DataFileCache;
import fr.xephi.authme.cache.backup.JsonCache;
import fr.xephi.authme.events.ResetInventoryEvent;
import fr.xephi.authme.events.StoreInventoryEvent;
import fr.xephi.authme.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ConcurrentHashMap;

public class LimboCache {

    private volatile static LimboCache singleton;
    public ConcurrentHashMap<String, LimboPlayer> cache;
    private JsonCache playerData;
    public AuthMe plugin;

    private LimboCache(AuthMe plugin) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
        this.playerData = new JsonCache(plugin);
    }

    public void addLimboPlayer(Player player) {
        String name = player.getName().toLowerCase();
        Location loc = player.getLocation();
        GameMode gameMode = player.getGameMode();
        ItemStack[] arm;
        ItemStack[] inv;
        boolean operator = false;
        String playerGroup = "";
        boolean flying = false;

        if (playerData.doesCacheExist(player)) {
            final StoreInventoryEvent event = new StoreInventoryEvent(player, playerData);
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (!event.isCancelled() && event.getInventory() != null && event.getArmor() != null) {
                inv = event.getInventory();
                arm = event.getArmor();
            } else {
                inv = null;
                arm = null;
            }
            DataFileCache cache = playerData.readCache(player);
            if (cache != null) {
                playerGroup = cache.getGroup();
                operator = cache.getOperator();
                flying = cache.isFlying();
            }
        } else {
            StoreInventoryEvent event = new StoreInventoryEvent(player);
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (!event.isCancelled() && event.getInventory() != null && event.getArmor() != null) {
                inv = event.getInventory();
                arm = event.getArmor();
            } else {
                inv = null;
                arm = null;
            }
            operator = player.isOp();
            flying = player.isFlying();
            if (plugin.permission != null) {
                try {
                    playerGroup = plugin.permission.getPrimaryGroup(player);
                } catch (UnsupportedOperationException e) {
                    ConsoleLogger.showError("Your permission system (" + plugin.permission.getName() + ") do not support Group system with that config... unhook!");
                    plugin.permission = null;
                }
            }
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
        cache.put(name, new LimboPlayer(name, loc, inv, arm, gameMode, operator, playerGroup, flying));
    }

    public void addLimboPlayer(Player player, String group) {
        cache.put(player.getName().toLowerCase(), new LimboPlayer(player.getName().toLowerCase(), group));
    }

    public void deleteLimboPlayer(String name) {
        cache.remove(name);
    }

    public LimboPlayer getLimboPlayer(String name) {
        return cache.get(name);
    }

    public boolean hasLimboPlayer(String name) {
        return cache.containsKey(name);
    }

    public static LimboCache getInstance() {
        if (singleton == null) {
            singleton = new LimboCache(AuthMe.getInstance());
        }
        return singleton;
    }

    public void updateLimboPlayer(Player player) {
        if (this.hasLimboPlayer(player.getName().toLowerCase())) {
            this.deleteLimboPlayer(player.getName().toLowerCase());
        }
        addLimboPlayer(player);
    }

}
