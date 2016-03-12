package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import org.bukkit.entity.Player;

import java.util.List;

public class FirstSpawnCommand extends PlayerCommand {

    @Override
    public void runCommand(Player player, List<String> arguments, CommandService commandService) {
        if (commandService.getSpawnLoader().getFirstSpawn() != null) {
            player.teleport(commandService.getSpawnLoader().getFirstSpawn());
        } else {
            player.sendMessage("[AuthMe] First spawn has failed, please try to define the first spawn");
        }
    }
}
