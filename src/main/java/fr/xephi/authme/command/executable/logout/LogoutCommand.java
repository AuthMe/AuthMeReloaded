package fr.xephi.authme.command.executable.logout;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Logout command.
 */
public class LogoutCommand extends PlayerCommand {

    @Override
    public void runCommand(Player player, List<String> arguments, CommandService commandService) {
        commandService.getManagement().performLogout(player);
    }
}
