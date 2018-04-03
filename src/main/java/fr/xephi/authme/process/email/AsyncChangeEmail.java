package fr.xephi.authme.process.email;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.EmailChangedEvent;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.service.bungeecord.BungeeSender;
import fr.xephi.authme.service.bungeecord.MessageType;
import org.bukkit.entity.Player;

import javax.inject.Inject;

/**
 * Async task for changing the email.
 */
public class AsyncChangeEmail implements AsynchronousProcess {

    @Inject
    private CommonService service;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private DataSource dataSource;

    @Inject
    private ValidationService validationService;

    @Inject
    private BungeeSender bungeeSender;
    
    @Inject
    private BukkitService bukkitService;

    AsyncChangeEmail() { }

    /**
     * Handles the request to change the player's email address.
     *
     * @param player the player to change the email for
     * @param oldEmail provided old email
     * @param newEmail provided new email
     */
    public void changeEmail(Player player, String oldEmail, String newEmail) {
        String playerName = player.getName().toLowerCase();
        if (playerCache.isAuthenticated(playerName)) {
            PlayerAuth auth = playerCache.getAuth(playerName);
            final String currentEmail = auth.getEmail();

            if (currentEmail == null) {
                service.send(player, MessageKey.USAGE_ADD_EMAIL);
            } else if (newEmail == null || !validationService.validateEmail(newEmail)) {
                service.send(player, MessageKey.INVALID_NEW_EMAIL);
            } else if (!oldEmail.equalsIgnoreCase(currentEmail)) {
                service.send(player, MessageKey.INVALID_OLD_EMAIL);
            } else if (!validationService.isEmailFreeForRegistration(newEmail, player)) {
                service.send(player, MessageKey.EMAIL_ALREADY_USED_ERROR);
            } else {
                saveNewEmail(auth, player, oldEmail, newEmail);
            }
        } else {
            outputUnloggedMessage(player);
        }
    }

    private void saveNewEmail(PlayerAuth auth, Player player, String oldEmail, String newEmail) {
        EmailChangedEvent event = bukkitService.createAndCallEvent(isAsync
            -> new EmailChangedEvent(player, oldEmail, newEmail, isAsync));
        if (event.isCancelled()) {
            ConsoleLogger.info("Could not change email for player '" + player + "' – event was cancelled");
            service.send(player, MessageKey.EMAIL_CHANGE_NOT_ALLOWED);
            return;
        }

        auth.setEmail(newEmail);
        if (dataSource.updateEmail(auth)) {
            playerCache.updatePlayer(auth);
            bungeeSender.sendAuthMeBungeecordMessage(MessageType.REFRESH_EMAIL, player.getName());
            service.send(player, MessageKey.EMAIL_CHANGED_SUCCESS);
        } else {
            service.send(player, MessageKey.ERROR);
        }
    }

    private void outputUnloggedMessage(Player player) {
        if (dataSource.isAuthAvailable(player.getName())) {
            service.send(player, MessageKey.LOGIN_MESSAGE);
        } else {
            service.send(player, MessageKey.REGISTER_MESSAGE);
        }
    }
}
