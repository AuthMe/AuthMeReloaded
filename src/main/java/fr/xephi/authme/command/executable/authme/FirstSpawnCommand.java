package fr.xephi.authme.command.executable.authme;

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
    public void runCommand(Player player, List<String> arguments) {
        if (spawnLoader.getFirstSpawn() == null) {
            player.sendMessage("[AuthMe] First spawn has failed, please try to define the first spawn");
        } else {
            player.teleportAsync(spawnLoader.getFirstSpawn());
        }
    }
}
