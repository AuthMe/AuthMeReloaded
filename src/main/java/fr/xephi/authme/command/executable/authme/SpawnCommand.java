package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.settings.Spawn;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SpawnCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        // Make sure the command executor is a player
        try {
            if (sender instanceof Player) {
                if (Spawn.getInstance().getSpawn() != null)
                    ((Player) sender).teleport(Spawn.getInstance().getSpawn());
                else sender.sendMessage("[AuthMe] Spawn has failed, please try to define the spawn");
            } else {
                sender.sendMessage("[AuthMe] Please use the command in game");
            }
        } catch (NullPointerException ex) {
            ConsoleLogger.showError(ex.getMessage());
        }
    }
}
