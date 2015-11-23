package fr.xephi.authme.command.executable.login;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 */
public class LoginCommand extends ExecutableCommand {

    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // Make sure the current command executor is a player
        if (!(sender instanceof Player)) {
            return true;
        }

        // Get the necessary objects
        final AuthMe plugin = AuthMe.getInstance();
        final Player player = (Player) sender;
        final String playerPass = commandArguments.get(0);

        // Log the player in
        plugin.getManagement().performLogin(player, playerPass, false);
        return true;
    }
}
