package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Returns a player's email.
 */
public class GetEmailCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        String playerName = arguments.isEmpty() ? sender.getName() : arguments.get(0);

        PlayerAuth auth = commandService.getDataSource().getAuth(playerName);
        if (auth == null) {
            commandService.send(sender, MessageKey.UNKNOWN_USER);
        } else {
            sender.sendMessage("[AuthMe] " + playerName + "'s email: " + auth.getEmail());
        }
    }
}
