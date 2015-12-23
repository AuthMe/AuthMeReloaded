package fr.xephi.authme.process.login;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
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
import fr.xephi.authme.util.Utils;
import fr.xephi.authme.util.Utils.GroupType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffectType;

/**
 */
public class ProcessSyncronousPlayerLogin implements Runnable {

    private final LimboPlayer limbo;
    private final Player player;
    private final String name;
    private final PlayerAuth auth;
    private final AuthMe plugin;
    private final DataSource database;
    private final PluginManager pm;
    private final JsonCache playerCache;

    /**
     * Constructor for ProcessSyncronousPlayerLogin.
     *
     * @param player Player
     * @param plugin AuthMe
     * @param data   DataSource
     */
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

    /**
     * Method getLimbo.
     *
     * @return LimboPlayer
     */
    public LimboPlayer getLimbo() {
        return limbo;
    }

    protected void restoreOpState() {
        player.setOp(limbo.getOperator());
    }

    protected void packQuitLocation() {
        Utils.packCoords(auth.getQuitLocX(), auth.getQuitLocY(), auth.getQuitLocZ(), auth.getWorld(), player);
    }

    protected void teleportBackFromSpawn() {
        AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(player, limbo.getLoc());
        pm.callEvent(tpEvent);
        if (!tpEvent.isCancelled() && tpEvent.getTo() != null) {
            player.teleport(tpEvent.getTo());
        }
    }

    protected void teleportToSpawn() {
        Location spawnL = plugin.getSpawnLocation(player);
        SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), spawnL, true);
        pm.callEvent(tpEvent);
        if (!tpEvent.isCancelled() && tpEvent.getTo() != null) {
            player.teleport(tpEvent.getTo());
        }
    }

    protected void restoreSpeedEffects() {
        if (Settings.isRemoveSpeedEnabled) {
            player.setWalkSpeed(0.2F);
            player.setFlySpeed(0.1F);
        }
    }

    protected void restoreInventory() {
        RestoreInventoryEvent event = new RestoreInventoryEvent(player);
        pm.callEvent(event);
        if (!event.isCancelled() && plugin.inventoryProtector != null) {
            plugin.inventoryProtector.sendInventoryPacket(player);
        }
    }

    protected void forceCommands() {
        for (String command : Settings.forceCommands) {
            player.performCommand(command.replace("%p", player.getName()));
        }
        for (String command : Settings.forceCommandsAsConsole) {
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command.replace("%p", player.getName()));
        }
    }

    protected void sendBungeeMessage() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF("AuthMe");
        out.writeUTF("login;" + name);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    /**
     * Method run.
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        // Limbo contains the State of the Player before /login
        if (limbo != null) {
            // Restore Op state and Permission Group
            restoreOpState();
            Utils.setGroup(player, GroupType.LOGGEDIN);

            if (!Settings.noTeleport) {
                if (Settings.isTeleportToSpawnEnabled && !Settings.isForceSpawnLocOnJoinEnabled && Settings.getForcedWorlds.contains(player.getWorld().getName())) {
                    if (Settings.isSaveQuitLocationEnabled && auth.getQuitLocY() != 0) {
                        packQuitLocation();
                    } else {
                        teleportBackFromSpawn();
                    }
                } else if (Settings.isForceSpawnLocOnJoinEnabled && Settings.getForcedWorlds.contains(player.getWorld().getName())) {
                    teleportToSpawn();
                } else if (Settings.isSaveQuitLocationEnabled && auth.getQuitLocY() != 0) {
                    packQuitLocation();
                } else {
                    teleportBackFromSpawn();
                }
            }

            if (Settings.protectInventoryBeforeLogInEnabled) {
                restoreInventory();
            }

            // Cleanup no longer used temporary data
            LimboCache.getInstance().deleteLimboPlayer(name);
            if (playerCache.doesCacheExist(player)) {
                playerCache.removeCache(player);
            }
        }

        // We can now display the join message (if delayed)
        String jm = AuthMePlayerListener.joinMessage.get(name);
        if (jm != null) {
            if (!jm.isEmpty()) {
                for (Player p : Utils.getOnlinePlayers()) {
                    if (p.isOnline())
                        p.sendMessage(jm);
                }
            }
            AuthMePlayerListener.joinMessage.remove(name);
        }

        restoreSpeedEffects();
        if (Settings.applyBlindEffect) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }

        // The Login event now fires (as intended) after everything is processed
        Bukkit.getServer().getPluginManager().callEvent(new LoginEvent(player, true));
        player.saveData();
        if (Settings.bungee)
            sendBungeeMessage();
        // Login is finish, display welcome message if we use email registration
        if (Settings.useWelcomeMessage && Settings.emailRegistration)
            if (Settings.broadcastWelcomeMessage) {
                for (String s : Settings.welcomeMsg) {
                    Bukkit.getServer().broadcastMessage(plugin.replaceAllInfo(s, player));
                }
            } else {
                for (String s : Settings.welcomeMsg) {
                    player.sendMessage(plugin.replaceAllInfo(s, player));
                }
            }

        // Login is now finish , we can force all commands
        forceCommands();
    }

}
