package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.PasswordRecoveryService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.service.ValidationService.ValidationResult;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * Command for changing password following successful recovery.
 */
public class SetPasswordCommand extends PlayerCommand {

    @Inject
    private DataSource dataSource;

    @Inject
    private CommonService commonService;

    @Inject
    private PasswordRecoveryService recoveryService;

    @Inject
    private PasswordSecurity passwordSecurity;

    @Inject
    private ValidationService validationService;

    @Override
    protected void runCommand(Player player, List<String> arguments) {
        if (recoveryService.canChangePassword(player)) {
            String name = player.getName();
            String password = arguments.get(0);

            ValidationResult result = validationService.validatePassword(password, name);
            if (!result.hasError()) {
                HashedPassword hashedPassword = passwordSecurity.computeHash(password, name);
                dataSource.updatePassword(name, hashedPassword);
                ConsoleLogger.info("Player '" + name + "' has changed their password from recovery");
                commonService.send(player, MessageKey.PASSWORD_CHANGED_SUCCESS);
            } else {
                commonService.send(player, result.getMessageKey(), result.getArgs());
            }
        }
    }
}
