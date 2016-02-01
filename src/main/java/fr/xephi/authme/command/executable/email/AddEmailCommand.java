package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import org.bukkit.entity.Player;

import java.util.List;

public class AddEmailCommand extends PlayerCommand {

    @Override
    public void runCommand(Player player, List<String> arguments, CommandService commandService) {
        String playerMail = arguments.get(0);
        String playerMailVerify = arguments.get(1);

        commandService.getManagement().performAddEmail(player, playerMail, playerMailVerify);
    }
}
