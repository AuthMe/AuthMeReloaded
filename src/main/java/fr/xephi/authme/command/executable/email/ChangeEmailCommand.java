package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Change email command.
 */
public class ChangeEmailCommand extends PlayerCommand {

    @Override
    public void runCommand(Player player, List<String> arguments, CommandService commandService) {
        String playerMailOld = arguments.get(0);
        String playerMailNew = arguments.get(1);

        commandService.getManagement().performChangeEmail(player, playerMailOld, playerMailNew);
    }
}
