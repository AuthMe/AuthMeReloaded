package fr.xephi.authme.command;

import fr.xephi.authme.util.StringUtils;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * The AuthMe command handler, responsible for mapping incoming commands to the correct {@link CommandDescription}
 * or to display help messages for unknown invocations.
 */
public class CommandHandler {

    private final CommandService commandService;

    /**
     * Create a command handler.
     */
    public CommandHandler(CommandService commandService) {
        this.commandService = commandService;
    }

    /**
     * Map a command that was invoked to the proper {@link CommandDescription} or return a useful error
     * message upon failure.
     *
     * @param sender             The command sender.
     * @param bukkitCommandLabel The command label (Bukkit).
     * @param bukkitArgs         The command arguments (Bukkit).
     *
     * @return True if the command was executed, false otherwise.
     */
    public boolean processCommand(CommandSender sender, String bukkitCommandLabel, String[] bukkitArgs) {
        // Add the Bukkit command label to the front so we get a list like [authme, register, bobby, mysecret]
        List<String> parts = skipEmptyArguments(bukkitArgs);
        parts.add(0, bukkitCommandLabel);

        FoundCommandResult result = commandService.mapPartsToCommand(sender, parts);
        if (FoundResultStatus.SUCCESS.equals(result.getResultStatus())) {
            executeCommand(sender, result);
        } else {
            commandService.outputMappingError(sender, result);
        }
        return !FoundResultStatus.MISSING_BASE_COMMAND.equals(result.getResultStatus());
    }

    private void executeCommand(CommandSender sender, FoundCommandResult result) {
        ExecutableCommand executableCommand = result.getCommandDescription().getExecutableCommand();
        List<String> arguments = result.getArguments();
        executableCommand.executeCommand(sender, arguments, commandService);
    }

    /**
     * Skip all entries of the given array that are simply whitespace.
     *
     * @param args The array to process
     * @return List of the items that are not empty
     */
    private static List<String> skipEmptyArguments(String[] args) {
        List<String> cleanArguments = new ArrayList<>();
        for (String argument : args) {
            if (!StringUtils.isEmpty(argument)) {
                cleanArguments.add(argument);
            }
        }
        return cleanArguments;
    }



}
