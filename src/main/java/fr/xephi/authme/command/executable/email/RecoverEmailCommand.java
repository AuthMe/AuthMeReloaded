package fr.xephi.authme.command.executable.email;

import ch.jalu.datasourcecolumns.data.DataSourceValue;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.PasswordRecoveryService;
import fr.xephi.authme.service.RecoveryCodeService;
import fr.xephi.authme.util.Utils;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * Command for password recovery by email.
 */
public class RecoverEmailCommand extends PlayerCommand {

    @Inject
    private CommonService commonService;

    @Inject
    private DataSource dataSource;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private EmailService emailService;

    @Inject
    private PasswordRecoveryService recoveryService;

    @Inject
    private RecoveryCodeService recoveryCodeService;

    @Inject
    private BukkitService bukkitService;

    @Override
    protected void runCommand(Player player, List<String> arguments) {
        final String playerMail = arguments.get(0);
        final String playerName = player.getName();

        if (!emailService.hasAllInformation()) {
            ConsoleLogger.warning("Mail API is not set");
            commonService.send(player, MessageKey.INCOMPLETE_EMAIL_SETTINGS);
            return;
        }
        if (playerCache.isAuthenticated(playerName)) {
            commonService.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
            return;
        }

        DataSourceValue<String> emailResult = dataSource.getEmail(playerName);
        if (!emailResult.rowExists()) {
            commonService.send(player, MessageKey.USAGE_REGISTER);
            return;
        }

        final String email = emailResult.getValue();
        if (Utils.isEmailEmpty(email) || !email.equalsIgnoreCase(playerMail)) {
            commonService.send(player, MessageKey.INVALID_EMAIL);
            return;
        }

        bukkitService.runTaskAsynchronously(() -> {
            if (recoveryCodeService.isRecoveryCodeNeeded()) {
                // Recovery code is needed; generate and send one
                recoveryService.createAndSendRecoveryCode(player, email);
            } else {
                // Code not needed, just send them a new password
                recoveryService.generateAndSendNewPassword(player, email);
            }
        });
    }

    @Override
    public MessageKey getArgumentsMismatchMessage() {
        return MessageKey.USAGE_RECOVER_EMAIL;
    }
}
