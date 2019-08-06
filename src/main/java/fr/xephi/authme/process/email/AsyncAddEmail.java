package fr.xephi.authme.process.email;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.EmailChangedEvent;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.service.bungeecord.BungeeSender;
import fr.xephi.authme.service.bungeecord.MessageType;
import fr.xephi.authme.util.Utils;
import org.bukkit.entity.Player;

import javax.inject.Inject;

/**
 * Async task to add an email to an account.
 */
public class AsyncAddEmail implements AsynchronousProcess {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(AsyncAddEmail.class);

    @Inject
    private CommonService service;

    @Inject
    private DataSource dataSource;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private ValidationService validationService;

    @Inject
    private BungeeSender bungeeSender;

    @Inject
    private BukkitService bukkitService;

    AsyncAddEmail() { }

    /**
     * Handles the request to add the given email to the player's account.
     *
     * @param player the player to add the email to
     * @param email the email to add
     */
    public void addEmail(Player player, String email) {
        String playerName = player.getName().toLowerCase();

        if (playerCache.isAuthenticated(playerName)) {
            PlayerAuth auth = playerCache.getAuth(playerName);
            final String currentEmail = auth.getEmail();

            if (!Utils.isEmailEmpty(currentEmail)) {
                service.send(player, MessageKey.USAGE_CHANGE_EMAIL);
            } else if (!validationService.validateEmail(email)) {
                service.send(player, MessageKey.INVALID_EMAIL);
            } else if (!validationService.isEmailFreeForRegistration(email, player)) {
                service.send(player, MessageKey.EMAIL_ALREADY_USED_ERROR);
            } else {
                EmailChangedEvent event = bukkitService.createAndCallEvent(isAsync
                    -> new EmailChangedEvent(player, null, email, isAsync));
                if (event.isCancelled()) {
                    logger.info("Could not add email to player '" + player + "' – event was cancelled");
                    service.send(player, MessageKey.EMAIL_ADD_NOT_ALLOWED);
                    return;
                }
                auth.setEmail(email);
                if (dataSource.updateEmail(auth)) {
                    playerCache.updatePlayer(auth);
                    bungeeSender.sendAuthMeBungeecordMessage(MessageType.REFRESH_EMAIL, playerName);
                    service.send(player, MessageKey.EMAIL_ADDED_SUCCESS);
                } else {
                    logger.warning("Could not save email for player '" + player + "'");
                    service.send(player, MessageKey.ERROR);
                }
            }
        } else {
            sendUnloggedMessage(player);
        }
    }

    private void sendUnloggedMessage(Player player) {
        if (dataSource.isAuthAvailable(player.getName())) {
            service.send(player, MessageKey.LOGIN_MESSAGE);
        } else {
            service.send(player, MessageKey.REGISTER_MESSAGE);
        }
    }

}
