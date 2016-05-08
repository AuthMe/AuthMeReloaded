package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.settings.SpawnLoader;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * Teleports the player to the first spawn.
 */
public class FirstSpawnCommand extends PlayerCommand {

    @Inject
    private SpawnLoader spawnLoader;

    @Override
    public void runCommand(Player player, List<String> arguments, CommandService commandService) {
        if (spawnLoader.getFirstSpawn() != null) {
            player.teleport(spawnLoader.getFirstSpawn());
        } else {
            player.sendMessage("[AuthMe] First spawn has failed, please try to define the first spawn");
        }
    }
}
