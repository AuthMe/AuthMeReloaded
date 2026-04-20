package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.command.CommandMapper;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.command.FoundCommandResult;
import fr.xephi.authme.command.help.HelpProvider;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

/**
 * Base command for /email, showing information about the child commands.
 */
public class EmailBaseCommand implements ExecutableCommand {

    @Inject
    private CommandMapper commandMapper;

    @Inject
    private HelpProvider helpProvider;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        FoundCommandResult result = commandMapper.mapPartsToCommand(sender, Collections.singletonList("email"));
        helpProvider.outputHelp(sender, result, HelpProvider.SHOW_CHILDREN);
    }
}
