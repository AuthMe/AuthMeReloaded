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

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments,
                               final CommandService commandService) {
        // Get the player and password
        final String playerName = arguments.get(0);
        final String playerPass = arguments.get(1);

        // Validate the password
        MessageKey passwordError = commandService.validatePassword(playerPass, playerName);
        if (passwordError != null) {
            commandService.send(sender, passwordError);
            return;
        }

        // Set the password
        final String playerNameLowerCase = playerName.toLowerCase();
        commandService.runTaskAsynchronously(new Runnable() {

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

                if (!dataSource.updatePassword(auth)) {
                    commandService.send(sender, MessageKey.ERROR);
                } else {
                    commandService.send(sender, MessageKey.PASSWORD_CHANGED_SUCCESS);
                    ConsoleLogger.info(playerNameLowerCase + "'s password changed");
                }
            }

        });
    }
}
