package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.settings.Spawn;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 */
public class SetFirstSpawnCommand extends ExecutableCommand {

    /**
     * Execute the command.
     *
     * @param sender           The command sender.
     * @param commandReference The command reference.
     * @param commandArguments The command arguments.
     * @return True if the command was executed successfully, false otherwise.
     */
    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
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
        return true;
    }
}
