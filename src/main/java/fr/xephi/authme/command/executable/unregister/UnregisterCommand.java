package fr.xephi.authme.command.executable.unregister;

import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.Management;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * Command for a player to unregister himself.
 */
public class UnregisterCommand extends PlayerCommand {

    @Inject
    private Management management;

    @Inject
    private CommandService commandService;

    @Inject
    private PlayerCache playerCache;

    @Override
    public void runCommand(Player player, List<String> arguments) {
        String playerPass = arguments.get(0);
        final String playerName = player.getName();

        // Make sure the player is authenticated
        if (!playerCache.isAuthenticated(playerName)) {
            commandService.send(player, MessageKey.NOT_LOGGED_IN);
            return;
        }

        // Unregister the player
        management.performUnregister(player, playerPass);
    }
}
