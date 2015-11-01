package fr.xephi.authme.command.executable;

import com.timvisee.dungeonmaze.command.CommandParts;
import com.timvisee.dungeonmaze.command.ExecutableCommand;
import com.timvisee.dungeonmaze.command.help.HelpProvider;
import org.bukkit.command.CommandSender;

public class HelpCommand extends ExecutableCommand {

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
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // Check whether quick help should be shown
        boolean quickHelp = commandArguments.getCount() == 0;

        // Set the proper command arguments for the quick help
        if(quickHelp)
            commandArguments = new CommandParts(commandReference.get(0));

        // Show the new help
        if(quickHelp)
            HelpProvider.showHelp(sender, commandReference, commandArguments, false, false, false, false, false, true);
        else
            HelpProvider.showHelp(sender, commandReference, commandArguments);

        // Return the result
        return true;
    }
}
