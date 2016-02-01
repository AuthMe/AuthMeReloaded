package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.settings.Spawn;
import org.bukkit.entity.Player;

import java.util.List;

public class FirstSpawnCommand extends PlayerCommand {

    @Override
    public void runCommand(Player player, List<String> arguments, CommandService commandService) {
        // Make sure the command executor is a player
        try {
            if (Spawn.getInstance().getFirstSpawn() != null) {
                player.teleport(Spawn.getInstance().getFirstSpawn());
            } else {
                player.sendMessage("[AuthMe] First spawn has failed, please try to define the first spawn");
            }
        } catch (NullPointerException ex) {
            // TODO ljacqu 20151119: Catching NullPointerException is never a good idea. Find what can cause one instead
            ConsoleLogger.showError(ex.getMessage());
        }
    }
}
