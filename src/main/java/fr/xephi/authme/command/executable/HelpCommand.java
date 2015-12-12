package fr.xephi.authme.command.executable;

import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.command.help.HelpProvider;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 */
public class HelpCommand extends ExecutableCommand {

    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // Check whether quick help should be shown
        List<String> arguments = commandArguments.getList();

        // Set the proper command arguments for the quick help and show it
        if (arguments.isEmpty()) {
            commandArguments = new CommandParts(commandReference.get(0));
            HelpProvider.showHelp(sender, commandReference, commandArguments, false, false, false, false, false, true);
        } else {
            HelpProvider.showHelp(sender, commandReference, commandArguments);
        }

        return true;
    }
}
