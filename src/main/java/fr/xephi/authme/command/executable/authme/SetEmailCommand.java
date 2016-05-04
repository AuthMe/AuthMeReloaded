package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;

/**
 * Admin command for setting an email to an account.
 */
public class SetEmailCommand implements ExecutableCommand {

    @Inject
    private DataSource dataSource;

    @Inject
    private PlayerCache playerCache;

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments,
                               final CommandService commandService) {
        // Get the player name and email address
        final String playerName = arguments.get(0);
        final String playerEmail = arguments.get(1);

        // Validate the email address
        if (!commandService.validateEmail(playerEmail)) {
            commandService.send(sender, MessageKey.INVALID_EMAIL);
            return;
        }

        commandService.runTaskAsynchronously(new Runnable() {
            @Override
            public void run() {
                // Validate the user
                PlayerAuth auth = dataSource.getAuth(playerName);
                if (auth == null) {
                    commandService.send(sender, MessageKey.UNKNOWN_USER);
                    return;
                } else if (!commandService.isEmailFreeForRegistration(playerEmail, sender)) {
                    commandService.send(sender, MessageKey.EMAIL_ALREADY_USED_ERROR);
                    return;
                }

                // Set the email address
                auth.setEmail(playerEmail);
                if (!dataSource.updateEmail(auth)) {
                    commandService.send(sender, MessageKey.ERROR);
                    return;
                }

                // Update the player cache
                if (playerCache.getAuth(playerName) != null) {
                    playerCache.updatePlayer(auth);
                }

                // Show a status message
                commandService.send(sender, MessageKey.EMAIL_CHANGED_SUCCESS);

            }
        });
    }
}
