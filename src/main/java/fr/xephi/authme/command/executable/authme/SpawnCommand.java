package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.settings.Spawn;
import org.bukkit.entity.Player;

import java.util.List;

public class SpawnCommand extends PlayerCommand {

    @Override
    public void runCommand(Player player, List<String> arguments, CommandService commandService) {
        try {
            if (Spawn.getInstance().getSpawn() != null) {
                player.teleport(Spawn.getInstance().getSpawn());
            } else {
                player.sendMessage("[AuthMe] Spawn has failed, please try to define the spawn");
            }
        } catch (NullPointerException ex) {
            ConsoleLogger.showError(ex.getMessage());
        }
    }
}
