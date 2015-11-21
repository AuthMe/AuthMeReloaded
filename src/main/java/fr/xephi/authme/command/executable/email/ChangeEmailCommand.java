package fr.xephi.authme.command.executable.email;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;

/**
 */
public class ChangeEmailCommand extends ExecutableCommand {

    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // Get the parameter values
        String playerMailOld = commandArguments.get(0);
        String playerMailNew = commandArguments.get(1);

        // Make sure the current command executor is a player
        if (!(sender instanceof Player)) {
            return true;
        }

        // Get the player instance and execute action
        final AuthMe plugin = AuthMe.getInstance();
        final Player player = (Player) sender;
        plugin.management.performChangeEmail(player, playerMailOld, playerMailNew);
        return true;
    }
}
