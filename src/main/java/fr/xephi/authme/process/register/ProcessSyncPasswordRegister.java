package fr.xephi.authme.process.register;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.events.RegisterEvent;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.SynchronousProcess;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.bungeecord.BungeeSender;
import fr.xephi.authme.settings.commandconfig.CommandManager;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.util.PlayerUtils;
import org.bukkit.entity.Player;

import javax.inject.Inject;

/**
 * Performs synchronous tasks after a successful {@link RegistrationType#PASSWORD password registration}.
 */
public class ProcessSyncPasswordRegister implements SynchronousProcess {

    @Inject
    private BungeeSender bungeeSender;

    @Inject
    private CommonService service;

    @Inject
    private LimboService limboService;

    @Inject
    private CommandManager commandManager;

    @Inject
    private BukkitService bukkitService;

    ProcessSyncPasswordRegister() {
    }

    /**
     * Request that the player log in.
     *
     * @param player the player
     */
    private void requestLogin(Player player) {
        limboService.replaceTasksAfterRegistration(player);

        if (player.isInsideVehicle() && player.getVehicle() != null) {
            player.getVehicle().eject();
        }
    }

    /**
     * Processes a player having registered with a password.
     *
     * @param player the newly registered player
     */
    public void processPasswordRegister(Player player) {
        service.send(player, MessageKey.REGISTER_SUCCESS);

        if (!service.getProperty(EmailSettings.MAIL_ACCOUNT).isEmpty()) {
            service.send(player, MessageKey.ADD_EMAIL_MESSAGE);
        }

        player.saveData();
        bukkitService.callEvent(new RegisterEvent(player));
        ConsoleLogger.fine(player.getName() + " registered " + PlayerUtils.getPlayerIp(player));

        // Kick Player after Registration is enabled, kick the player
        if (service.getProperty(RegistrationSettings.FORCE_KICK_AFTER_REGISTER)) {
            player.kickPlayer(service.retrieveSingleMessage(player, MessageKey.REGISTER_SUCCESS));
            return;
        }

        // Register is now finished; we can force all commands
        commandManager.runCommandsOnRegister(player);

        // Request login after registration
        if (service.getProperty(RegistrationSettings.FORCE_LOGIN_AFTER_REGISTER)) {
            requestLogin(player);
            return;
        }

        // Send Bungee stuff. The service will check if it is enabled or not.
        bungeeSender.connectPlayerOnLogin(player);
    }
}
