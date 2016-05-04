package fr.xephi.authme.process.email;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.process.Process;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import org.bukkit.entity.Player;

/**
 * Async task for changing the email.
 */
public class AsyncChangeEmail implements Process {

    private final Player player;
    private final String oldEmail;
    private final String newEmail;
    private final ProcessService service;
    private final PlayerCache playerCache;
    private final DataSource dataSource;

    public AsyncChangeEmail(Player player, String oldEmail, String newEmail, DataSource dataSource,
                            PlayerCache playerCache, ProcessService service) {
        this.player = player;
        this.oldEmail = oldEmail;
        this.newEmail = newEmail;
        this.playerCache = playerCache;
        this.dataSource = dataSource;
        this.service = service;
    }

    @Override
    public void run() {
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
                saveNewEmail(auth);
            }
        } else {
            outputUnloggedMessage();
        }
    }

    private void saveNewEmail(PlayerAuth auth) {
        auth.setEmail(newEmail);
        if (dataSource.updateEmail(auth)) {
            playerCache.updatePlayer(auth);
            service.send(player, MessageKey.EMAIL_CHANGED_SUCCESS);
        } else {
            service.send(player, MessageKey.ERROR);
        }
    }

    private void outputUnloggedMessage() {
        if (dataSource.isAuthAvailable(player.getName())) {
            service.send(player, MessageKey.LOGIN_MESSAGE);
        } else if (service.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)) {
            service.send(player, MessageKey.REGISTER_EMAIL_MESSAGE);
        } else {
            service.send(player, MessageKey.REGISTER_MESSAGE);
        }
    }
}
