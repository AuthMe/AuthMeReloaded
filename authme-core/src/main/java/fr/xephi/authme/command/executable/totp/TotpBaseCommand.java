package fr.xephi.authme.command.executable.totp;

import fr.xephi.authme.command.CommandMapper;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.command.FoundCommandResult;
import fr.xephi.authme.command.help.HelpProvider;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

/**
 * Base command for /totp.
 */
public class TotpBaseCommand implements ExecutableCommand {

    @Inject
    private CommandMapper commandMapper;

    @Inject
    private HelpProvider helpProvider;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        FoundCommandResult result = commandMapper.mapPartsToCommand(sender, Collections.singletonList("totp"));
        helpProvider.outputHelp(sender, result, HelpProvider.SHOW_CHILDREN);
    }
}
