package fr.xephi.authme.process.email;

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
 * Async task for changing the email.
 */
public class AsyncChangeEmail implements AsynchronousProcess {

    @Inject
    private ProcessService service;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private DataSource dataSource;

    AsyncChangeEmail() { }

    public void changeEmail(Player player, String oldEmail, String newEmail) {
        String playerName = player.getName().toLowerCase();
        if (playerCache.isAuthenticated(playerName)) {
            PlayerAuth auth = playerCache.getAuth(playerName);
            final String currentEmail = auth.getEmail();

            if (currentEmail == null) {
                service.send(player, MessageKey.USAGE_ADD_EMAIL);
            } else if (newEmail == null || !service.validateEmail(newEmail)) {
                service.send(player, MessageKey.INVALID_NEW_EMAIL);
            } else if (!oldEmail.equals(currentEmail)) {
                service.send(player, MessageKey.INVALID_OLD_EMAIL);
            } else if (!service.isEmailFreeForRegistration(newEmail, player)) {
                service.send(player, MessageKey.EMAIL_ALREADY_USED_ERROR);
            } else {
                saveNewEmail(auth, player, newEmail);
            }
        } else {
            outputUnloggedMessage(player);
        }
    }

    private void saveNewEmail(PlayerAuth auth, Player player, String newEmail) {
        auth.setEmail(newEmail);
        if (dataSource.updateEmail(auth)) {
            playerCache.updatePlayer(auth);
            service.send(player, MessageKey.EMAIL_CHANGED_SUCCESS);
        } else {
            service.send(player, MessageKey.ERROR);
        }
    }

    private void outputUnloggedMessage(Player player) {
        if (dataSource.isAuthAvailable(player.getName())) {
            service.send(player, MessageKey.LOGIN_MESSAGE);
        } else if (service.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)) {
            service.send(player, MessageKey.REGISTER_EMAIL_MESSAGE);
        } else {
            service.send(player, MessageKey.REGISTER_MESSAGE);
        }
    }
}
