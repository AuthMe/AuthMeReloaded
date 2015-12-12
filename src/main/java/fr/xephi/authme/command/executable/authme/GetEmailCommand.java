package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import org.bukkit.command.CommandSender;

import java.util.List;

public class GetEmailCommand extends ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        String playerName = arguments.isEmpty() ? sender.getName() : arguments.get(0);

        // Get the authenticated user
        AuthMe plugin = AuthMe.getInstance();
        Messages m = plugin.getMessages();
        PlayerAuth auth = plugin.database.getAuth(playerName.toLowerCase());
        if (auth == null) {
            m.send(sender, MessageKey.UNKNOWN_USER);
            return;
        }

        // Show the email address
        sender.sendMessage("[AuthMe] " + playerName + "'s email: " + auth.getEmail());
    }
}
