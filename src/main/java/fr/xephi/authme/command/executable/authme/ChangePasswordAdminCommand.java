package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.service.ValidationService.ValidationResult;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;

/**
 * Admin command for changing a player's password.
 */
public class ChangePasswordAdminCommand implements ExecutableCommand {

    @Inject
    private PasswordSecurity passwordSecurity;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private DataSource dataSource;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private ValidationService validationService;

    @Inject
    private CommonService commonService;

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments) {
        // Get the player and password
        final String playerName = arguments.get(0);
        final String playerPass = arguments.get(1);

        // Validate the password
        ValidationResult validationResult = validationService.validatePassword(playerPass, playerName);
        if (validationResult.hasError()) {
            commonService.send(sender, validationResult.getMessageKey(), validationResult.getArgs());
            return;
        }

        // Set the password
        bukkitService.runTaskOptionallyAsync(() -> changePassword(playerName.toLowerCase(), playerPass, sender));
    }

    /**
     * Changes the password of the given player to the given password.
     *
     * @param nameLowercase the name of the player
     * @param password the password to set
     * @param sender the sender initiating the password change
     */
    private void changePassword(String nameLowercase, String password, CommandSender sender) {
        if (!isNameRegistered(nameLowercase)) {
            commonService.send(sender, MessageKey.UNKNOWN_USER);
            return;
        }

        HashedPassword hashedPassword = passwordSecurity.computeHash(password, nameLowercase);
        if (dataSource.updatePassword(nameLowercase, hashedPassword)) {
            commonService.send(sender, MessageKey.PASSWORD_CHANGED_SUCCESS);
            ConsoleLogger.info(sender.getName() + " changed password of " + nameLowercase);
        } else {
            commonService.send(sender, MessageKey.ERROR);
        }
    }

    private boolean isNameRegistered(String nameLowercase) {
        return playerCache.isAuthenticated(nameLowercase) || dataSource.isAuthAvailable(nameLowercase);
    }
}
