package fr.xephi.authme.command.executable.logout;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;

public class LogoutCommand extends ExecutableCommand {

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

        // Logout the player
        plugin.management.performLogout(player);
        return true;
    }
}
