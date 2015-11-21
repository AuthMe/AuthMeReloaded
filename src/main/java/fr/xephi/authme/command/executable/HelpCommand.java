package fr.xephi.authme.command.executable;

import org.bukkit.command.CommandSender;

import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.command.help.HelpProvider;

/**
 */
public class HelpCommand extends ExecutableCommand {

    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // Check whether quick help should be shown
        boolean quickHelp = commandArguments.getCount() == 0;

        // Set the proper command arguments for the quick help and show it
        if (quickHelp) {
            commandArguments = new CommandParts(commandReference.get(0));
            HelpProvider.showHelp(sender, commandReference, commandArguments, false, false, false, false, false, true);
        } else {
            HelpProvider.showHelp(sender, commandReference, commandArguments);
        }

        return true;
    }
}
