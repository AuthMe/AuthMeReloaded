package fr.xephi.authme.process.login;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffectType;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.Utils;
import fr.xephi.authme.Utils.GroupType;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.backup.JsonCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.AuthMeTeleportEvent;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.RestoreInventoryEvent;
import fr.xephi.authme.events.SpawnTeleportEvent;
import fr.xephi.authme.listener.AuthMePlayerListener;
import fr.xephi.authme.settings.Settings;

public class ProcessSyncronousPlayerLogin implements Runnable {

    private LimboPlayer limbo;
    private Player player;
    private String name;
    private PlayerAuth auth;
    private AuthMe plugin;
    private DataSource database;
    private PluginManager pm;
    private JsonCache playerCache;

    public ProcessSyncronousPlayerLogin(Player player, AuthMe plugin,
            DataSource data) {
        this.plugin = plugin;
        this.database = data;
        this.pm = plugin.getServer().getPluginManager();
        this.player = player;
        this.name = player.getName().toLowerCase();
        this.limbo = LimboCache.getInstance().getLimboPlayer(name);
        this.auth = database.getAuth(name);
        this.playerCache = new JsonCache();
    }

    public LimboPlayer getLimbo() {
        return limbo;
    }

    protected void restoreOpState() {
        player.setOp(limbo.getOperator());
        if (player.getGameMode() != GameMode.CREATIVE && !Settings.isMovementAllowed) {
            if (limbo.getGameMode() != GameMode.CREATIVE) {
                player.setAllowFlight(limbo.isFlying());
                player.setFlying(limbo.isFlying());
            } else {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        }
    }

    protected void packQuitLocation() {
        Utils.packCoords(auth.getQuitLocX(), auth.getQuitLocY(), auth.getQuitLocZ(), auth.getWorld(), player);
    }

    protected void teleportBackFromSpawn() {
        AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(player, limbo.getLoc());
        pm.callEvent(tpEvent);
        if (!tpEvent.isCancelled()) {
            Location fLoc = tpEvent.getTo();
            if (!fLoc.getChunk().isLoaded()) {
                fLoc.getChunk().load();
            }
            player.teleport(fLoc);
        }
    }

    protected void teleportToSpawn() {
        Location spawnL = plugin.getSpawnLocation(player);
        SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), spawnL, true);
        pm.callEvent(tpEvent);
        if (!tpEvent.isCancelled()) {
            Location fLoc = tpEvent.getTo();
            if (!fLoc.getChunk().isLoaded()) {
                fLoc.getChunk().load();
            }
            player.teleport(fLoc);
        }
    }

    protected void restoreInventory() {
        RestoreInventoryEvent event = new RestoreInventoryEvent(player);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            plugin.api.setPlayerInventory(player, event.getInventory(), event.getArmor());
            plugin.inventoryProtector.sendInventoryPacket(player);
        }
    }

    protected void forceCommands() {
        for (String command : Settings.forceCommands) {
            try {
                player.performCommand(command.replace("%p", player.getName()));
            } catch (Exception ignored) {
            }
        }
        for (String command : Settings.forceCommandsAsConsole) {
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command.replace("%p", player.getName()));
        }
    }

    @Override
    public void run() {
        // Limbo contains the State of the Player before /login
        if (limbo != null) {
            // Op & Flying
            restoreOpState();

            /*
             * Restore Inventories and GameMode We need to restore them before
             * teleport the player Cause in AuthMePlayerListener, we call
             * ProtectInventoryEvent after Teleporting Also it's the current
             * world inventory !
             */
            player.setGameMode(limbo.getGameMode());
            // Inventory - Make it after restore GameMode , cause we need to
            // restore the
            // right inventory in the right gamemode
            if (Settings.protectInventoryBeforeLogInEnabled  && plugin.inventoryProtector != null) {
                restoreInventory();
            }
            if (Settings.forceOnlyAfterLogin) {
                player.setGameMode(GameMode.SURVIVAL);
            }

            if (!Settings.noTeleport) {
                // Teleport
                if (Settings.isTeleportToSpawnEnabled && !Settings.isForceSpawnLocOnJoinEnabled && Settings.getForcedWorlds.contains(player.getWorld().getName())) {
                    if (Settings.isSaveQuitLocationEnabled && auth.getQuitLocY() != 0) {
                        packQuitLocation();
                    } else {
                        teleportBackFromSpawn();
                    }
                } else
                    if (Settings.isForceSpawnLocOnJoinEnabled && Settings.getForcedWorlds.contains(player.getWorld().getName())) {
                    teleportToSpawn();
                } else if (Settings.isSaveQuitLocationEnabled && auth.getQuitLocY() != 0) {
                    packQuitLocation();
                } else {
                    teleportBackFromSpawn();
                }
            }

            // Re-Force Survival GameMode if we need due to world change
            // specification
            if (Settings.isForceSurvivalModeEnabled)
                Utils.forceGM(player);

            // Restore Permission Group
            Utils.setGroup(player, GroupType.LOGGEDIN);

            // Cleanup no longer used temporary data
            LimboCache.getInstance().deleteLimboPlayer(name);
            if (playerCache.doesCacheExist(player)) {
                playerCache.removeCache(player);
            }
        }

        // We can now display the join message
        if (AuthMePlayerListener.joinMessage.containsKey(name) && AuthMePlayerListener.joinMessage.get(name) != null && !AuthMePlayerListener.joinMessage.get(name).isEmpty()) {
            for (Player p : Utils.getOnlinePlayers()) {
                if (p.isOnline())
                    p.sendMessage(AuthMePlayerListener.joinMessage.get(name));
            }
            AuthMePlayerListener.joinMessage.remove(name);
        }

        if (Settings.applyBlindEffect)
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        if (!Settings.isMovementAllowed && Settings.isRemoveSpeedEnabled) {
            player.setWalkSpeed(0.2f);
            player.setFlySpeed(0.1f);
        }

        // The Loginevent now fires (as intended) after everything is processed
        Bukkit.getServer().getPluginManager().callEvent(new LoginEvent(player, true));
        player.saveData();

        // Login is finish, display welcome message
        if (Settings.useWelcomeMessage)
            if (Settings.broadcastWelcomeMessage) {
                for (String s : Settings.welcomeMsg) {
                    Bukkit.getServer().broadcastMessage(plugin.replaceAllInfos(s, player));
                }
            } else {
                for (String s : Settings.welcomeMsg) {
                    player.sendMessage(plugin.replaceAllInfos(s, player));
                }
            }

        // Login is now finish , we can force all commands
        forceCommands();
    }

}
