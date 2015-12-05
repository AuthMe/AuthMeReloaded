package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 */
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
        // Get the player name
        List<String> arguments = commandArguments.getList();
        String playerName = sender.getName();
        if (arguments.size() >= 1)
            playerName = commandArguments.get(0);

        // Get the authenticated user
        AuthMe plugin = AuthMe.getInstance();
        Messages m = plugin.getMessages();
        PlayerAuth auth = plugin.database.getAuth(playerName.toLowerCase());
        if (auth == null) {
            m.send(sender, MessageKey.UNKNOWN_USER);
            return true;
        }

        // Show the email address
        sender.sendMessage("[AuthMe] " + playerName + "'s email: " + auth.getEmail());
        return true;
    }
}
