package fr.xephi.authme.command.executable.unregister;

import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.output.MessageKey;
import org.bukkit.entity.Player;

import java.util.List;

public class UnregisterCommand extends PlayerCommand {

    @Override
    public void runCommand(Player player, List<String> arguments, CommandService commandService) {
        String playerPass = arguments.get(0);
        final String playerNameLowerCase = player.getName().toLowerCase();

        // Make sure the player is authenticated
        if (!PlayerCache.getInstance().isAuthenticated(playerNameLowerCase)) {
            commandService.send(player, MessageKey.NOT_LOGGED_IN);
            return;
        }

        // Unregister the player
        commandService.getManagement().performUnregister(player, playerPass, false);
    }
}
