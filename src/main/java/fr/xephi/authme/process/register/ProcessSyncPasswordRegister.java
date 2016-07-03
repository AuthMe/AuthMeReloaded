package fr.xephi.authme.process.register;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.RestoreInventoryEvent;
import fr.xephi.authme.listener.protocollib.ProtocolLibService;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.permission.AuthGroupType;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.process.SynchronousProcess;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.task.LimboPlayerTaskManager;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;

import static fr.xephi.authme.settings.properties.RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN;

/**
 */
public class ProcessSyncPasswordRegister implements SynchronousProcess {

    @Inject
    private AuthMe plugin;

    @Inject
    private ProcessService service;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private ProtocolLibService protocolLibService;

    @Inject
    private LimboCache limboCache;

    @Inject
    private LimboPlayerTaskManager limboPlayerTaskManager;

    ProcessSyncPasswordRegister() { }


    private void sendBungeeMessage(Player player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF("AuthMe");
        out.writeUTF("register;" + player.getName());
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    private void forceCommands(Player player) {
        for (String command : service.getProperty(RegistrationSettings.FORCE_REGISTER_COMMANDS)) {
            player.performCommand(command.replace("%p", player.getName()));
        }
        for (String command : service.getProperty(RegistrationSettings.FORCE_REGISTER_COMMANDS_AS_CONSOLE)) {
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
                    command.replace("%p", player.getName()));
        }
    }

    /**
     * Request that the player log in.
     *
     * @param player the player
     */
    private void requestLogin(Player player) {
        final String name = player.getName().toLowerCase();
        Utils.teleportToSpawn(player);

        limboCache.updateLimboPlayer(player);
        limboPlayerTaskManager.registerTimeoutTask(player);
        limboPlayerTaskManager.registerMessageTask(name, true);

        if (player.isInsideVehicle() && player.getVehicle() != null) {
            player.getVehicle().eject();
        }
    }

    public void processPasswordRegister(Player player) {
        final String name = player.getName().toLowerCase();
        LimboPlayer limbo = limboCache.getLimboPlayer(name);
        if (limbo != null) {
            Utils.teleportToSpawn(player);

            if (service.getProperty(PROTECT_INVENTORY_BEFORE_LOGIN)) {
                RestoreInventoryEvent event = new RestoreInventoryEvent(player);
                bukkitService.callEvent(event);
                if (!event.isCancelled()) {
                    player.updateInventory();
                }
            }

            limboCache.deleteLimboPlayer(player);
        }

        if (!Settings.getRegisteredGroup.isEmpty()) {
            service.setGroup(player, AuthGroupType.REGISTERED);
        }

        service.send(player, MessageKey.REGISTER_SUCCESS);

        if (!service.getProperty(EmailSettings.MAIL_ACCOUNT).isEmpty()) {
            service.send(player, MessageKey.ADD_EMAIL_MESSAGE);
        }

        if (service.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }

        // The LoginEvent now fires (as intended) after everything is processed
        bukkitService.callEvent(new LoginEvent(player));
        player.saveData();

        if (!service.getProperty(SecuritySettings.REMOVE_SPAM_FROM_CONSOLE)) {
            ConsoleLogger.info(player.getName() + " registered " + Utils.getPlayerIp(player));
        }

        // Kick Player after Registration is enabled, kick the player
        if (service.getProperty(RegistrationSettings.FORCE_KICK_AFTER_REGISTER)) {
            player.kickPlayer(service.retrieveSingleMessage(MessageKey.REGISTER_SUCCESS));
            return;
        }

        // Register is now finished; we can force all commands
        forceCommands(player);

        // Request login after registration
        if (service.getProperty(RegistrationSettings.FORCE_LOGIN_AFTER_REGISTER)) {
            requestLogin(player);
            return;
        }

        if (service.getProperty(HooksSettings.BUNGEECORD)) {
            sendBungeeMessage(player);
        }

        sendTo(player);
    }

    private void sendTo(Player player) {
        if (!service.getProperty(HooksSettings.BUNGEECORD_SERVER).isEmpty()) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(service.getProperty(HooksSettings.BUNGEECORD_SERVER));
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        }
    }
}
