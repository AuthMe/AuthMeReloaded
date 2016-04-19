package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import org.bukkit.entity.Player;

import java.util.List;

public class SpawnCommand extends PlayerCommand {

    @Override
    public void runCommand(Player player, List<String> arguments, CommandService commandService) {
        if (commandService.getSpawnLoader().getSpawn() != null) {
            player.teleport(commandService.getSpawnLoader().getSpawn());
        } else {
            player.sendMessage("[AuthMe] Spawn has failed, please try to define the spawn");
        }
    }
}
