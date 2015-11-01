package fr.xephi.authme.command.executable.login;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LoginCommand extends ExecutableCommand {

    /**
     * Execute the command.
     *
     * @param sender The command sender.
     * @param commandReference The command reference.
     * @param commandArguments The command arguments.
     *
     * @return True if the command was executed successfully, false otherwise.
     */
    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // AuthMe plugin instance
        final AuthMe plugin = AuthMe.getInstance();

        // Make sure the current command executor is a player
        if(!(sender instanceof Player)) {
            return true;
        }

        // Get the player instance
        final Player player = (Player) sender;

        // Get the password
        String playerPass = commandArguments.get(0);

        // Login the player
        plugin.management.performLogin(player, playerPass, false);
        return true;
    }
}
