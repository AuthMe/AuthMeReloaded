package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Removes the stored last position of a user or of all.
 */
public class PurgeLastPositionCommand implements ExecutableCommand {

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments, CommandService commandService) {
        String playerName = arguments.isEmpty() ? sender.getName() : arguments.get(0);

        if ("*".equals(playerName)) {
            for (PlayerAuth auth : commandService.getDataSource().getAllAuths()) {
                resetLastPosition(auth);
                commandService.getDataSource().updateQuitLoc(auth);
            }
            sender.sendMessage("All players last position locations are now reset");
        } else {
            // Get the user auth and make sure the user exists
            PlayerAuth auth = commandService.getDataSource().getAuth(playerName);
            if (auth == null) {
                commandService.send(sender, MessageKey.UNKNOWN_USER);
                return;
            }

            resetLastPosition(auth);
            commandService.getDataSource().updateQuitLoc(auth);
            sender.sendMessage(playerName + "'s last position location is now reset");
        }
    }

    private static void resetLastPosition(PlayerAuth auth) {
        auth.setQuitLocX(0d);
        auth.setQuitLocY(0d);
        auth.setQuitLocZ(0d);
        auth.setWorld("world");
    }
}
