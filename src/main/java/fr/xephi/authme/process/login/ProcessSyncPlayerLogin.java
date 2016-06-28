package fr.xephi.authme.process.login;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.RestoreInventoryEvent;
import fr.xephi.authme.listener.AuthMePlayerListener;
import fr.xephi.authme.listener.protocollib.ProtocolLibService;
import fr.xephi.authme.permission.AuthGroupType;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.process.SynchronousProcess;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.TeleportationService;
import org.apache.commons.lang.reflect.MethodUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;

import static fr.xephi.authme.settings.properties.PluginSettings.KEEP_COLLISIONS_DISABLED;
import static fr.xephi.authme.settings.properties.RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN;

public class ProcessSyncPlayerLogin implements SynchronousProcess {

    private static final boolean RESTORE_COLLISIONS = MethodUtils
        .getAccessibleMethod(LivingEntity.class, "setCollidable", new Class[]{}) != null;

    @Inject
    private AuthMe plugin;

    @Inject
    private ProcessService service;

    @Inject
    private LimboCache limboCache;

    @Inject
    private DataSource dataSource;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private ProtocolLibService protocolLibService;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private TeleportationService teleportationService;

    ProcessSyncPlayerLogin() { }

    private void restoreInventory(Player player) {
        RestoreInventoryEvent event = new RestoreInventoryEvent(player);
        pluginManager.callEvent(event);
        if (!event.isCancelled()) {
            player.updateInventory();
        }
    }

    private void forceCommands(Player player) {
        for (String command : service.getProperty(RegistrationSettings.FORCE_COMMANDS)) {
            player.performCommand(command.replace("%p", player.getName()));
        }
        for (String command : service.getProperty(RegistrationSettings.FORCE_COMMANDS_AS_CONSOLE)) {
            Bukkit.getServer().dispatchCommand(
                Bukkit.getServer().getConsoleSender(), command.replace("%p", player.getName()));
        }
    }

    public void processPlayerLogin(Player player) {
        final String name = player.getName().toLowerCase();
        // Limbo contains the State of the Player before /login
        final LimboPlayer limbo = limboCache.getLimboPlayer(name);
        final PlayerAuth auth = dataSource.getAuth(name);

        if (limbo != null) {
            // Restore Op state and Permission Group
            player.setOp(limbo.isOperator());
            // Restore primary group
            service.setGroup(player, AuthGroupType.LOGGED_IN);
            // Restore can-fly state
            player.setAllowFlight(limbo.isCanFly());

            // Restore speed
            if (!service.getProperty(RestrictionSettings.ALLOW_UNAUTHED_MOVEMENT)
                && service.getProperty(RestrictionSettings.REMOVE_SPEED)) {
                player.setWalkSpeed(limbo.getWalkSpeed());
                player.setFlySpeed(0.2F);
            }

            teleportationService.teleportOnLogin(player, auth, limbo);

            if (RESTORE_COLLISIONS && !service.getProperty(KEEP_COLLISIONS_DISABLED)) {
                player.setCollidable(true);
            }

            if (service.getProperty(PROTECT_INVENTORY_BEFORE_LOGIN)) {
                restoreInventory(player);
            }

            if (service.getProperty(RestrictionSettings.HIDE_TABLIST_BEFORE_LOGIN)) {
                protocolLibService.sendTabList(player);
            }

            // Clean up no longer used temporary data
            limboCache.deleteLimboPlayer(player);
        }

        // We can now display the join message (if delayed)
        String jm = AuthMePlayerListener.joinMessage.get(name);
        if (jm != null) {
            if (!jm.isEmpty()) {
                for (Player p : bukkitService.getOnlinePlayers()) {
                    if (p.isOnline()) {
                        p.sendMessage(jm);
                    }
                }
            }
            AuthMePlayerListener.joinMessage.remove(name);
        }

        if (service.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }

        // The Login event now fires (as intended) after everything is processed
        bukkitService.callEvent(new LoginEvent(player));
        player.saveData();
        sendBungeeMessage(player);

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
        forceCommands(player);

        sendTo(player);
    }

    private void sendTo(Player player) {
        if(!service.getProperty(HooksSettings.BUNGEECORD)) {
            return;
        }
        if(service.getProperty(HooksSettings.BUNGEECORD_SERVER).isEmpty()) {
            return;
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(service.getProperty(HooksSettings.BUNGEECORD_SERVER));
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    private void sendBungeeMessage(Player player) {
        if(!service.getProperty(HooksSettings.BUNGEECORD)) {
            return;
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF("AuthMe");
        out.writeUTF("login;" + player.getName());
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }
}
