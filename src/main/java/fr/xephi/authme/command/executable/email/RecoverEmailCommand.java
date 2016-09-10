package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.EmailRecoveryData;
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

        EmailRecoveryData recoveryData = dataSource.getEmailRecoveryData(playerName);
        if (recoveryData == null) {
            commandService.send(player, MessageKey.REGISTER_EMAIL_MESSAGE);
            return;
        }

        final String email = recoveryData.getEmail();
        if (email == null || !email.equalsIgnoreCase(playerMail) || "your@email.com".equalsIgnoreCase(email)) {
            commandService.send(player, MessageKey.INVALID_EMAIL);
            return;
        }

        if (arguments.size() == 1) {
            // Process /email recover addr@example.com
            createAndSendRecoveryCode(playerName, recoveryData);
        } else {
            // Process /email recover addr@example.com 12394
            processRecoveryCode(player, arguments.get(1), recoveryData);
        }
    }

    private void createAndSendRecoveryCode(String name, EmailRecoveryData recoveryData) {
        // TODO #472: Add configurations
        String recoveryCode = RandomString.generateHex(8);
        long expiration = System.currentTimeMillis() + (3 * 60 * 60_000L); // 3 hours
        dataSource.setRecoveryCode(name, recoveryCode, expiration);
        sendMailSsl.sendRecoveryCode(recoveryData.getEmail(), recoveryCode);
    }

    private void processRecoveryCode(Player player, String code, EmailRecoveryData recoveryData) {
        if (!code.equals(recoveryData.getRecoveryCode())) {
            player.sendMessage("The recovery code is not correct! Use /email recovery [email] to generate a new one");
            return;
        }

        final String name = player.getName();
        String thePass = RandomString.generate(commandService.getProperty(EmailSettings.RECOVERY_PASSWORD_LENGTH));
        HashedPassword hashNew = passwordSecurity.computeHash(thePass, name);
        dataSource.updatePassword(name, hashNew);
        dataSource.removeRecoveryCode(name);
        sendMailSsl.sendPasswordMail(name, recoveryData.getEmail(), thePass);
        commandService.send(player, MessageKey.RECOVERY_EMAIL_SENT_MESSAGE);
    }
}
