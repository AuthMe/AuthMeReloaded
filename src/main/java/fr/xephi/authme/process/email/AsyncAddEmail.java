package fr.xephi.authme.process.email;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.process.Process;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.util.Utils;
import org.bukkit.entity.Player;

/**
 * Async task to add an email to an account.
 */
public class AsyncAddEmail implements Process {

    private final Player player;
    private final String email;
    private final ProcessService service;
    private final DataSource dataSource;
    private final PlayerCache playerCache;

    public AsyncAddEmail(Player player, String email, DataSource dataSource, PlayerCache playerCache,
                         ProcessService service) {
        this.player = player;
        this.email = email;
        this.dataSource = dataSource;
        this.playerCache = playerCache;
        this.service = service;
    }

    @Override
    public void run() {
        String playerName = player.getName().toLowerCase();

        if (playerCache.isAuthenticated(playerName)) {
            PlayerAuth auth = playerCache.getAuth(playerName);
            final String currentEmail = auth.getEmail();

            if (currentEmail != null && !"your@email.com".equals(currentEmail)) {
                service.send(player, MessageKey.USAGE_CHANGE_EMAIL);
            } else if (!Utils.isEmailCorrect(email, service.getSettings())) {
                service.send(player, MessageKey.INVALID_EMAIL);
            } else if (dataSource.isEmailStored(email)) {
                service.send(player, MessageKey.EMAIL_ALREADY_USED_ERROR);
            } else {
                auth.setEmail(email);
                if (dataSource.updateEmail(auth)) {
                    playerCache.updatePlayer(auth);
                    service.send(player, MessageKey.EMAIL_ADDED_SUCCESS);
                } else {
                    ConsoleLogger.showError("Could not save email for player '" + player + "'");
                    service.send(player, MessageKey.ERROR);
                }
            }
        } else {
            sendUnloggedMessage(dataSource);
        }
    }

    private void sendUnloggedMessage(DataSource dataSource) {
        if (dataSource.isAuthAvailable(player.getName())) {
            service.send(player, MessageKey.LOGIN_MESSAGE);
        } else if (service.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)) {
            service.send(player, MessageKey.REGISTER_EMAIL_MESSAGE);
        } else {
            service.send(player, MessageKey.REGISTER_MESSAGE);
        }
    }

}
