package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.command.FoundCommandResult;
import fr.xephi.authme.command.help.HelpProvider;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Base command for /email, showing information about the child commands.
 */
public class EmailBaseCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        FoundCommandResult result = commandService.mapPartsToCommand(sender, Collections.singletonList("email"));
        commandService.outputHelp(sender, result, HelpProvider.SHOW_CHILDREN);
    }
}
