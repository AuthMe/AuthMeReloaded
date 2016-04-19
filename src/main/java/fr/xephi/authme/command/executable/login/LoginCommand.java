package fr.xephi.authme.command.executable.login;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Login command.
 */
public class LoginCommand extends PlayerCommand {

    @Override
    public void runCommand(Player player, List<String> arguments, CommandService commandService) {
        final String password = arguments.get(0);
        commandService.getManagement().performLogin(player, password, false);
    }
}
