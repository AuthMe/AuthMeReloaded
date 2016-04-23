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
import fr.xephi.authme.process.Process;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.Utils;
import fr.xephi.authme.util.Utils.GroupType;
import org.apache.commons.lang.reflect.MethodUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffectType;

import static fr.xephi.authme.settings.properties.PluginSettings.KEEP_COLLISIONS_DISABLED;
import static fr.xephi.authme.settings.properties.RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN;

public class ProcessSyncPlayerLogin implements Process {

    private final LimboPlayer limbo;
    private final Player player;
    private final String name;
    private final PlayerAuth auth;
    private final AuthMe plugin;
    private final PluginManager pm;
    private final JsonCache playerCache;
    private final ProcessService service;

    private final boolean restoreCollisions = MethodUtils
            .getAccessibleMethod(LivingEntity.class, "setCollidable", new Class[]{}) != null;

    /**
     * Constructor for ProcessSyncPlayerLogin.
     *
     * @param player Player
     * @param plugin AuthMe
     * @param database DataSource
     * @param service The process service
     */
    public ProcessSyncPlayerLogin(Player player, AuthMe plugin, DataSource database, ProcessService service) {
        this.plugin = plugin;
        this.pm = plugin.getServer().getPluginManager();
        this.player = player;
        this.name = player.getName().toLowerCase();
        this.limbo = LimboCache.getInstance().getLimboPlayer(name);
        this.auth = database.getAuth(name);
        this.playerCache = new JsonCache();
        this.service = service;
    }

    public LimboPlayer getLimbo() {
        return limbo;
    }

    private void restoreOpState() {
        player.setOp(limbo.isOperator());
    }

    private void packQuitLocation() {
        Utils.packCoords(auth.getQuitLocX(), auth.getQuitLocY(), auth.getQuitLocZ(), auth.getWorld(), player);
    }

    private void teleportBackFromSpawn() {
        AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(player, limbo.getLoc());
        pm.callEvent(tpEvent);
        if (!tpEvent.isCancelled() && tpEvent.getTo() != null) {
            player.teleport(tpEvent.getTo());
        }
    }

    private void teleportToSpawn() {
        Location spawnL = plugin.getSpawnLocation(player);
        SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), spawnL, true);
        pm.callEvent(tpEvent);
        if (!tpEvent.isCancelled() && tpEvent.getTo() != null) {
            player.teleport(tpEvent.getTo());
        }
    }

    private void restoreSpeedEffects() {
        if (!service.getProperty(RestrictionSettings.ALLOW_UNAUTHED_MOVEMENT) && service.getProperty(RestrictionSettings.REMOVE_SPEED)) {
            player.setWalkSpeed(0.2F);
            player.setFlySpeed(0.1F);
        }
    }

    private void restoreInventory() {
        RestoreInventoryEvent event = new RestoreInventoryEvent(player);
        pm.callEvent(event);
        if (!event.isCancelled() && plugin.inventoryProtector != null) {
            plugin.inventoryProtector.sendInventoryPacket(player);
        }
    }

    private void forceCommands() {
        for (String command : service.getProperty(RegistrationSettings.FORCE_COMMANDS)) {
            player.performCommand(command.replace("%p", player.getName()));
        }
        for (String command : service.getProperty(RegistrationSettings.FORCE_COMMANDS_AS_CONSOLE)) {
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command.replace("%p", player.getName()));
        }
    }

    private void sendBungeeMessage() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF("AuthMe");
        out.writeUTF("login;" + name);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

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

            if (restoreCollisions && !service.getProperty(KEEP_COLLISIONS_DISABLED)) {
                player.setCollidable(true);
            }

            if (service.getProperty(PROTECT_INVENTORY_BEFORE_LOGIN)) {
                restoreInventory();
            }

            if (service.getProperty(RestrictionSettings.HIDE_TABLIST_BEFORE_LOGIN) && plugin.tablistHider != null) {
                plugin.tablistHider.sendTablist(player);
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
                for (Player p : service.getOnlinePlayers()) {
                    if (p.isOnline()) {
                        p.sendMessage(jm);
                    }
                }
            }
            AuthMePlayerListener.joinMessage.remove(name);
        }

        restoreSpeedEffects();
        if (service.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }

        // The Login event now fires (as intended) after everything is processed
        Bukkit.getServer().getPluginManager().callEvent(new LoginEvent(player));
        player.saveData();
        if (service.getProperty(HooksSettings.BUNGEECORD)) {
            sendBungeeMessage();
        }
        // Login is done, display welcome message
        if (service.getProperty(RegistrationSettings.USE_WELCOME_MESSAGE)) {
            if (service.getProperty(RegistrationSettings.BROADCAST_WELCOME_MESSAGE)) {
                for (String s : service.getSettings().getWelcomeMessage()) {
                    Bukkit.getServer().broadcastMessage(plugin.replaceAllInfo(s, player));
                }
            } else {
                for (String s : service.getSettings().getWelcomeMessage()) {
                    player.sendMessage(plugin.replaceAllInfo(s, player));
                }
            }
        }

        // Login is now finished; we can force all commands
        forceCommands();

        sendTo();
    }

    private void sendTo() {
        if (!service.getProperty(HooksSettings.BUNGEECORD_SERVER).isEmpty()) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(service.getProperty(HooksSettings.BUNGEECORD_SERVER));
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        }
    }
}
