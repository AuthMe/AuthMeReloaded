package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import org.bukkit.command.CommandSender;

public class SwitchAntiBotCommand extends ExecutableCommand {

    /**
     * Execute the command.
     *
     * @param sender           The command sender.
     * @param commandReference The command reference.
     * @param commandArguments The command arguments.
     *
     * @return True if the command was executed successfully, false otherwise.
     */
    @Override
    public boolean executeCommand(final CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // AuthMe plugin instance
        final AuthMe plugin = AuthMe.getInstance();

        // Get the new state
        String newState = plugin.getAntiBotModMode() ? "OFF" : "ON";
        if(commandArguments.getCount() >= 1)
            newState = commandArguments.get(0);

        // Enable the mod
        if(newState.equalsIgnoreCase("ON")) {
            plugin.switchAntiBotMod(true);
            sender.sendMessage("[AuthMe] AntiBotMod enabled");
            return true;
        }

        // Disable the mod
        if(newState.equalsIgnoreCase("OFF")) {
            plugin.switchAntiBotMod(false);
            sender.sendMessage("[AuthMe] AntiBotMod disabled");
            return true;
        }

        // Invalid command arguments, return false
        return false;
    }
}
