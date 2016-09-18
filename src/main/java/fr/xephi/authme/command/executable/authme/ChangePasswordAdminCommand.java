package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.ValidationService;
import fr.xephi.authme.util.ValidationService.ValidationResult;
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
    private CommandService commandService;

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments) {
        // Get the player and password
        final String playerName = arguments.get(0);
        final String playerPass = arguments.get(1);

        // Validate the password
        ValidationResult validationResult = validationService.validatePassword(playerPass, playerName);
        if (validationResult.hasError()) {
            commandService.send(sender, validationResult.getMessageKey(), validationResult.getArgs());
            return;
        }

        // Set the password
        final String playerNameLowerCase = playerName.toLowerCase();
        bukkitService.runTaskOptionallyAsync(new Runnable() {

            @Override
            public void run() {
                PlayerAuth auth = null;
                if (playerCache.isAuthenticated(playerNameLowerCase)) {
                    auth = playerCache.getAuth(playerNameLowerCase);
                } else if (dataSource.isAuthAvailable(playerNameLowerCase)) {
                    auth = dataSource.getAuth(playerNameLowerCase);
                }
                if (auth == null) {
                    commandService.send(sender, MessageKey.UNKNOWN_USER);
                    return;
                }

                HashedPassword hashedPassword = passwordSecurity.computeHash(playerPass, playerNameLowerCase);
                auth.setPassword(hashedPassword);

                if (dataSource.updatePassword(auth)) {
                    commandService.send(sender, MessageKey.PASSWORD_CHANGED_SUCCESS);
                    ConsoleLogger.info(sender.getName() + " changed password of " + playerNameLowerCase);
                } else {
                    commandService.send(sender, MessageKey.ERROR);
                }
            }

        });
    }
}
