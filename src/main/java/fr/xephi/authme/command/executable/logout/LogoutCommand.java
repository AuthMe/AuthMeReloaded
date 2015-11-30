package fr.xephi.authme.command.executable.logout;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.util.Wrapper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 */
public class LogoutCommand extends ExecutableCommand {

    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // Make sure the current command executor is a player
        if (!(sender instanceof Player)) {
            return true;
        }

        // Get the player instance
        final AuthMe plugin = Wrapper.getInstance().getAuthMe();
        final Player player = (Player) sender;

        // Logout the player
        plugin.getManagement().performLogout(player);
        return true;
    }
}
