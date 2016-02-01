package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import org.bukkit.command.CommandSender;

import java.util.List;

public class PurgeLastPositionCommand implements ExecutableCommand {

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments, CommandService commandService) {
        String playerName = arguments.isEmpty() ? sender.getName() : arguments.get(0);
        String playerNameLowerCase = playerName.toLowerCase();

        if (playerNameLowerCase.equalsIgnoreCase("*"))
        {
        	for (PlayerAuth auth : commandService.getDataSource().getAllAuths())
        	{
                // Set the last position
                auth.setQuitLocX(0D);
                auth.setQuitLocY(0D);
                auth.setQuitLocZ(0D);
                auth.setWorld("world");
                commandService.getDataSource().updateQuitLoc(auth);
        	}
        	sender.sendMessage("All players last position locations are now reset");
        }
        else
        {
            // Get the user auth and make sure the user exists
            PlayerAuth auth = commandService.getDataSource().getAuth(playerNameLowerCase);
            if (auth == null) {
                commandService.send(sender, MessageKey.UNKNOWN_USER);
                return;
            }

            // Set the last position
            auth.setQuitLocX(0D);
            auth.setQuitLocY(0D);
            auth.setQuitLocZ(0D);
            auth.setWorld("world");
            commandService.getDataSource().updateQuitLoc(auth);

            // Show a status message
            sender.sendMessage(playerNameLowerCase + "'s last position location is now reset");
        }

    }
}
