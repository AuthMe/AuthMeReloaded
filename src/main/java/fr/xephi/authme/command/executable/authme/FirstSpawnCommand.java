package fr.xephi.authme.command.executable.authme;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.settings.Spawn;

/**
 */
public class FirstSpawnCommand extends ExecutableCommand {

    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // Make sure the command executor is a player
        try {
            if (sender instanceof Player) {
                if (Spawn.getInstance().getFirstSpawn() != null)
                    ((Player) sender).teleport(Spawn.getInstance().getFirstSpawn());
                else sender.sendMessage("[AuthMe] First spawn has failed, please try to define the first spawn");
            } else {
                sender.sendMessage("[AuthMe] Please use that command in game");
            }
        } catch (NullPointerException ex) {
            // TODO ljacqu 20151119: Catching NullPointerException is never a good idea. Find what can cause one instead
            ConsoleLogger.showError(ex.getMessage());
        }
        return true;
    }
}
