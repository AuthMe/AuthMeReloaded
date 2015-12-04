package fr.xephi.authme.command.executable.unregister;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 */
public class UnregisterCommand extends ExecutableCommand {

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
        // AuthMe plugin instance
        final AuthMe plugin = AuthMe.getInstance();

        // Messages instance
        final Messages m = plugin.getMessages();

        // Make sure the current command executor is a player
        if (!(sender instanceof Player)) {
            return true;
        }

        // Get the password
        String playerPass = commandArguments.get(0);

        // Get the player instance and name
        final Player player = (Player) sender;
        final String playerNameLowerCase = player.getName().toLowerCase();

        // Make sure the player is authenticated
        if (!PlayerCache.getInstance().isAuthenticated(playerNameLowerCase)) {
            m.send(player, MessageKey.NOT_LOGGED_IN);
            return true;
        }

        // Unregister the player
        plugin.getManagement().performUnregister(player, playerPass, false);
        return true;
    }
}
