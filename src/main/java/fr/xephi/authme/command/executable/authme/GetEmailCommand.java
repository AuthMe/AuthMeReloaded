package fr.xephi.authme.command.executable.authme;

import org.bukkit.command.CommandSender;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.settings.Messages;

public class GetEmailCommand extends ExecutableCommand {

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
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // AuthMe plugin instance
        AuthMe plugin = AuthMe.getInstance();

        // Messages instance
        Messages m = Messages.getInstance();

        // Get the player name
        String playerName = sender.getName();
        if(commandArguments.getCount() >= 1)
            playerName = commandArguments.get(0);

        // Get the authenticated user
        PlayerAuth auth = plugin.database.getAuth(playerName.toLowerCase());
        if (auth == null) {
            m.send(sender, "unknown_user");
            return true;
        }

        // Show the email address
        sender.sendMessage("[AuthMe] " + playerName + "'s email: " + auth.getEmail());
        return true;
    }
}
