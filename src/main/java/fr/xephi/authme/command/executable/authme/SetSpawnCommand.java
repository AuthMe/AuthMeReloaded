package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import org.bukkit.entity.Player;

import java.util.List;

public class SetSpawnCommand extends PlayerCommand {

    @Override
    public void runCommand(Player player, List<String> arguments, CommandService commandService) {
        if (commandService.getSpawnLoader().setSpawn(player.getLocation())) {
            player.sendMessage("[AuthMe] Correctly defined new spawn point");
        } else {
            player.sendMessage("[AuthMe] SetSpawn has failed, please retry");
        }
    }
}
