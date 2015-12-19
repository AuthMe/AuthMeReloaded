package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.Settings;
import org.bukkit.command.CommandSender;

import java.util.List;

public class SetEmailCommand extends ExecutableCommand {

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments) {
        // AuthMe plugin instance
        final AuthMe plugin = AuthMe.getInstance();

        // Messages instance
        final Messages m = plugin.getMessages();

        // Get the player name and email address
        final String playerName = arguments.get(0);
        final String playerEmail = arguments.get(1);

        // Validate the email address
        if (!Settings.isEmailCorrect(playerEmail)) {
            m.send(sender, MessageKey.INVALID_EMAIL);
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                // Validate the user
                PlayerAuth auth = plugin.database.getAuth(playerName);
                if (auth == null) {
                    m.send(sender, MessageKey.UNKNOWN_USER);
                    return;
                }

                // Set the email address

                auth.setEmail(playerEmail);
                if (!plugin.database.updateEmail(auth)) {
                    m.send(sender, MessageKey.ERROR);
                    return;
                }

                // Update the player cache
                if (PlayerCache.getInstance().getAuth(playerName) != null) {
                    PlayerCache.getInstance().updatePlayer(auth);
                }

                // Show a status message
                m.send(sender, MessageKey.EMAIL_CHANGED_SUCCESS);

            }
        });
    }
}
