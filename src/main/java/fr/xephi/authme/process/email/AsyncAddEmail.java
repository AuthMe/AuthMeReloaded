package fr.xephi.authme.process.email;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import org.bukkit.entity.Player;

import javax.inject.Inject;

/**
 * Async task to add an email to an account.
 */
public class AsyncAddEmail implements AsynchronousProcess {

    @Inject
    private ProcessService service;

    @Inject
    private DataSource dataSource;

    @Inject
    private PlayerCache playerCache;

    AsyncAddEmail() { }

    public void addEmail(Player player, String email) {
        String playerName = player.getName().toLowerCase();

        if (playerCache.isAuthenticated(playerName)) {
            PlayerAuth auth = playerCache.getAuth(playerName);
            final String currentEmail = auth.getEmail();

            if (currentEmail != null && !"your@email.com".equals(currentEmail)) {
                service.send(player, MessageKey.USAGE_CHANGE_EMAIL);
            } else if (!service.validateEmail(email)) {
                service.send(player, MessageKey.INVALID_EMAIL);
            } else if (!service.isEmailFreeForRegistration(email, player)) {
                service.send(player, MessageKey.EMAIL_ALREADY_USED_ERROR);
            } else {
                auth.setEmail(email);
                if (dataSource.updateEmail(auth)) {
                    playerCache.updatePlayer(auth);
                    service.send(player, MessageKey.EMAIL_ADDED_SUCCESS);
                } else {
                    ConsoleLogger.warning("Could not save email for player '" + player + "'");
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
        } else if (service.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)) {
            service.send(player, MessageKey.REGISTER_EMAIL_MESSAGE);
        } else {
            service.send(player, MessageKey.REGISTER_MESSAGE);
        }
    }

}
