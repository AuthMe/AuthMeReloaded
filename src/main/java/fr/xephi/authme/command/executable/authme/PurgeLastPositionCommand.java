package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class PurgeLastPositionCommand implements ExecutableCommand {

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments, CommandService commandService) {
        final AuthMe plugin = AuthMe.getInstance();
        final Messages m = plugin.getMessages();

        String playerName = arguments.isEmpty() ? sender.getName() : arguments.get(0);

        // Get the player
        String playerNameLowerCase = playerName.toLowerCase();

        // Purge the last position of the player
        try {
            // Get the user auth and make sure the user exists
            PlayerAuth auth = plugin.database.getAuth(playerNameLowerCase);
            if (auth == null) {
                m.send(sender, MessageKey.UNKNOWN_USER);
                return;
            }

            // Set the last position
            auth.setQuitLocX(0D);
            auth.setQuitLocY(0D);
            auth.setQuitLocZ(0D);
            auth.setWorld("world");
            plugin.database.updateQuitLoc(auth);

            // Show a status message
            sender.sendMessage(playerNameLowerCase + "'s last position location is now reset");

        } catch (Exception e) {
            ConsoleLogger.showError("An error occurred while trying to reset location or player do not exist, please see below: ");
            ConsoleLogger.showError(e.getMessage());
            if (sender instanceof Player)
                sender.sendMessage("An error occurred while trying to reset location or player do not exist, please see logs");
        }
    }
}
