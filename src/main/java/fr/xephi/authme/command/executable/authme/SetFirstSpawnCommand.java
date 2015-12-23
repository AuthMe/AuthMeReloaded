package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.settings.Spawn;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SetFirstSpawnCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        try {
            if (sender instanceof Player) {
                if (Spawn.getInstance().setFirstSpawn(((Player) sender).getLocation()))
                    sender.sendMessage("[AuthMe] Correctly defined new first spawn point");
                else sender.sendMessage("[AuthMe] SetFirstSpawn has failed, please retry");
            } else {
                sender.sendMessage("[AuthMe] Please use that command in game");
            }
        } catch (NullPointerException ex) {
            ConsoleLogger.showError(ex.getMessage());
        }
    }
}
