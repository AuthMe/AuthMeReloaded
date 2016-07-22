package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.mail.SendMailSSL;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.properties.EmailSettings;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

public class RecoverEmailCommand extends PlayerCommand {

    @Inject
    private PasswordSecurity passwordSecurity;

    @Inject
    private CommandService commandService;

    @Inject
    private DataSource dataSource;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private SendMailSSL sendMailSsl;

    @Override
    public void runCommand(Player player, List<String> arguments) {
        final String playerMail = arguments.get(0);
        final String playerName = player.getName();

        if (!sendMailSsl.hasAllInformation()) {
            ConsoleLogger.warning("Mail API is not set");
            commandService.send(player, MessageKey.INCOMPLETE_EMAIL_SETTINGS);
            return;
        }
        if (dataSource.isAuthAvailable(playerName)) {
            if (playerCache.isAuthenticated(playerName)) {
                commandService.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
                return;
            }

            PlayerAuth auth;
            if (playerCache.isAuthenticated(playerName)) {
                auth = playerCache.getAuth(playerName);
            } else if (dataSource.isAuthAvailable(playerName)) {
                auth = dataSource.getAuth(playerName);
            } else {
                commandService.send(player, MessageKey.UNKNOWN_USER);
                return;
            }

            if (!playerMail.equalsIgnoreCase(auth.getEmail()) || "your@email.com".equalsIgnoreCase(playerMail)
                || "your@email.com".equalsIgnoreCase(auth.getEmail())) {
                commandService.send(player, MessageKey.INVALID_EMAIL);
                return;
            }

            String thePass = RandomString.generate(commandService.getProperty(EmailSettings.RECOVERY_PASSWORD_LENGTH));
            HashedPassword hashNew = passwordSecurity.computeHash(thePass, playerName);
            auth.setPassword(hashNew);
            dataSource.updatePassword(auth);
            sendMailSsl.sendPasswordMail(auth, thePass);
            commandService.send(player, MessageKey.RECOVERY_EMAIL_SENT_MESSAGE);
        } else {
            commandService.send(player, MessageKey.REGISTER_EMAIL_MESSAGE);
        }
    }
}
