package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.settings.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 */
public class PurgeLastPositionCommand extends ExecutableCommand {

    /**
     * Execute the command.
     *
     * @param sender           The command sender.
     * @param commandReference The command reference.
     * @param commandArguments The command arguments.
     *
     * @return True if the command was executed successfully, false otherwise.
     */
    @Override
    public boolean executeCommand(final CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // AuthMe plugin instance
        final AuthMe plugin = AuthMe.getInstance();

        // Messages instance
        final Messages m = Messages.getInstance();

        // Get the player
        String playerName = sender.getName();
        if (commandArguments.getCount() >= 1)
            playerName = commandArguments.get(0);
        String playerNameLowerCase = playerName.toLowerCase();

        // Purge the last position of the player
        try {
            // Get the user auth and make sure the user exists
            PlayerAuth auth = plugin.database.getAuth(playerNameLowerCase);
            if (auth == null) {
                m.send(sender, "unknown_user");
                return true;
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
        return true;
    }
}
