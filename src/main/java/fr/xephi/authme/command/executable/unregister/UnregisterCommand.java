package fr.xephi.authme.command.executable.unregister;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class UnregisterCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        // Make sure the current command executor is a player
        if (!(sender instanceof Player)) {
            return;
        }

        final AuthMe plugin = AuthMe.getInstance();
        final Messages m = plugin.getMessages();

        // Get the password
        String playerPass = arguments.get(0);

        // Get the player instance and name
        final Player player = (Player) sender;
        final String playerNameLowerCase = player.getName().toLowerCase();

        // Make sure the player is authenticated
        if (!PlayerCache.getInstance().isAuthenticated(playerNameLowerCase)) {
            m.send(player, MessageKey.NOT_LOGGED_IN);
            return;
        }

        // Unregister the player
        plugin.getManagement().performUnregister(player, playerPass, false);
    }
}
