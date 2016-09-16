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
import fr.xephi.authme.service.RecoveryCodeManager;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

import static fr.xephi.authme.settings.properties.EmailSettings.RECOVERY_PASSWORD_LENGTH;

/**
 * Command for password recovery by email.
 */
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

    @Inject
    private RecoveryCodeManager recoveryCodeManager;

    @Override
    public void runCommand(Player player, List<String> arguments) {
        final String playerMail = arguments.get(0);
        final String playerName = player.getName();

        if (!sendMailSsl.hasAllInformation()) {
            ConsoleLogger.warning("Mail API is not set");
            commandService.send(player, MessageKey.INCOMPLETE_EMAIL_SETTINGS);
            return;
        }
        if (playerCache.isAuthenticated(playerName)) {
            commandService.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
            return;
        }

        PlayerAuth auth = dataSource.getAuth(playerName); // TODO: Create method to get email only
        if (auth == null) {
            commandService.send(player, MessageKey.REGISTER_EMAIL_MESSAGE);
            return;
        }

        final String email = auth.getEmail();
        if (email == null || !email.equalsIgnoreCase(playerMail) || "your@email.com".equalsIgnoreCase(email)) {
            commandService.send(player, MessageKey.INVALID_EMAIL);
            return;
        }

        if (recoveryCodeManager.isRecoveryCodeNeeded()) {
            // Process /email recovery addr@example.com
            if (arguments.size() == 1) {
                createAndSendRecoveryCode(player, email);
            } else {
                // Process /email recovery addr@example.com 12394
                processRecoveryCode(player, arguments.get(1), email);
            }
        } else {
            generateAndSendNewPassword(player, email);
        }
    }

    private void createAndSendRecoveryCode(Player player, String email) {
        String recoveryCode = recoveryCodeManager.generateCode(player.getName());
        sendMailSsl.sendRecoveryCode(player.getName(), email, recoveryCode);
        commandService.send(player, MessageKey.RECOVERY_CODE_SENT);
    }

    private void processRecoveryCode(Player player, String code, String email) {
        final String name = player.getName();
        if (!recoveryCodeManager.isCodeValid(name, code)) {
            commandService.send(player, MessageKey.INCORRECT_RECOVERY_CODE);
            return;
        }

        generateAndSendNewPassword(player, email);
        recoveryCodeManager.removeCode(name);
    }

    private void generateAndSendNewPassword(Player player, String email) {
        String name = player.getName();
        String thePass = RandomString.generate(commandService.getProperty(RECOVERY_PASSWORD_LENGTH));
        HashedPassword hashNew = passwordSecurity.computeHash(thePass, name);

        dataSource.updatePassword(name, hashNew);
        sendMailSsl.sendPasswordMail(name, email, thePass);
        commandService.send(player, MessageKey.RECOVERY_EMAIL_SENT_MESSAGE);
    }
}
