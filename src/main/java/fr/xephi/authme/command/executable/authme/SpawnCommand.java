package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.settings.SpawnLoader;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

public class SpawnCommand extends PlayerCommand {

    @Inject
    private SpawnLoader spawnLoader;

    @Override
    public void runCommand(Player player, List<String> arguments) {
        if (spawnLoader.getSpawn() == null) {
            messages.send(player, MessageKey.SPAWN_NOT_DEFINED);
        } else {
            player.teleport(spawnLoader.getSpawn());
        }
    }
}
