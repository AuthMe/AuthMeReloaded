package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Base command for /email, showing information about the child commands.
 */
public class EmailBaseCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        // FIXME #306 use getCommandService().getHelpProvider();
        // FIXME #306 HelpProvider.printHelp()
    }
}
