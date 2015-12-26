package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.settings.Spawn;
import org.bukkit.entity.Player;

import java.util.List;

public class SetFirstSpawnCommand extends PlayerCommand {

    @Override
    public void runCommand(Player player, List<String> arguments, CommandService commandService) {
        try {
            if (Spawn.getInstance().setFirstSpawn(player.getLocation())) {
                player.sendMessage("[AuthMe] Correctly defined new first spawn point");
            } else {
                player.sendMessage("[AuthMe] SetFirstSpawn has failed, please retry");
            }
        } catch (NullPointerException ex) {
            ConsoleLogger.showError(ex.getMessage());
        }
    }
}
