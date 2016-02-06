package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.util.Utils;
import org.bukkit.command.CommandSender;

import java.util.List;

public class SetEmailCommand implements ExecutableCommand {

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments,
                               final CommandService commandService) {
        // Get the player name and email address
        final String playerName = arguments.get(0);
        final String playerEmail = arguments.get(1);

        // Validate the email address
        if (!Utils.isEmailCorrect(playerEmail, commandService.getSettings())) {
            commandService.send(sender, MessageKey.INVALID_EMAIL);
            return;
        }

        commandService.runTaskAsynchronously(new Runnable() {
            @Override
            public void run() {
                // Validate the user
                PlayerAuth auth = commandService.getDataSource().getAuth(playerName);
                if (auth == null) {
                    commandService.send(sender, MessageKey.UNKNOWN_USER);
                    return;
                }

                // Set the email address
                auth.setEmail(playerEmail);
                if (!commandService.getDataSource().updateEmail(auth)) {
                    commandService.send(sender, MessageKey.ERROR);
                    return;
                }

                // Update the player cache
                if (PlayerCache.getInstance().getAuth(playerName) != null) {
                    PlayerCache.getInstance().updatePlayer(auth);
                }

                // Show a status message
                commandService.send(sender, MessageKey.EMAIL_CHANGED_SUCCESS);

            }
        });
    }
}
