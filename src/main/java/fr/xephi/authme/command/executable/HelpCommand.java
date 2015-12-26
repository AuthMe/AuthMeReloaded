package fr.xephi.authme.command.executable;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.CommandUtils;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.command.FoundCommandResult;
import fr.xephi.authme.command.FoundResultStatus;
import fr.xephi.authme.command.help.HelpProvider;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;

import static fr.xephi.authme.command.FoundResultStatus.MISSING_BASE_COMMAND;
import static fr.xephi.authme.command.FoundResultStatus.UNKNOWN_LABEL;

public class HelpCommand implements ExecutableCommand {

    // Convention: arguments is not the actual invoked arguments but the command that was invoked,
    // e.g. "/authme help register" would typically be arguments = [register], but here we pass [authme, register]
    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        FoundCommandResult result = commandService.mapPartsToCommand(sender, arguments);

        FoundResultStatus resultStatus = result.getResultStatus();
        if (MISSING_BASE_COMMAND.equals(resultStatus)) {
            sender.sendMessage(ChatColor.DARK_RED + "Could not get base command");
            return;
        } else if (UNKNOWN_LABEL.equals(resultStatus)) {
            if (result.getCommandDescription() == null) {
                sender.sendMessage(ChatColor.DARK_RED + "Unknown command");
                return;
            } else {
                sender.sendMessage(ChatColor.GOLD + "Assuming " + ChatColor.WHITE
                    + CommandUtils.constructCommandPath(result.getCommandDescription()));
            }
        }

        int mappedCommandLevel = foundCommandResult.getCommandDescription().getLabelCount();
        if (mappedCommandLevel == 1) {
            commandService.outputHelp(sender, result, HelpProvider.SHOW_CHILDREN);
        } else {
            commandService.outputHelp(sender, result, HelpProvider.ALL_OPTIONS);
        }
    }

}
