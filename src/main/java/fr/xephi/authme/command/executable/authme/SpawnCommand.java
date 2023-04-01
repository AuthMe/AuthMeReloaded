package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.SpawnLoader;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

public class SpawnCommand extends PlayerCommand {

    @Inject
    private BukkitService bukkitService;
    @Inject
    private SpawnLoader spawnLoader;

    @Override
    public void runCommand(Player player, List<String> arguments) {
        if (spawnLoader.getSpawn() == null) {
            player.sendMessage("[AuthMe] Spawn has failed, please try to define the spawn");
        } else {
            bukkitService.teleport(player, spawnLoader.getSpawn());
        }
    }
}
