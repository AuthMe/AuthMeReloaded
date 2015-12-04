package fr.xephi.authme.command;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.help.HelpProvider;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The AuthMe command handler, responsible for mapping incoming commands to the correct {@link CommandDescription}
 * or to display help messages for unknown invocations.
 */
public class CommandHandler {

    /**
     * The threshold for assuming an existing command. If the difference is below this value, we assume
     * that the user meant the similar command and we will run it.
     */
    private static final double ASSUME_COMMAND_THRESHOLD = 0.12;

    /**
     * The threshold for suggesting a similar command. If the difference is below this value, we will
     * ask the player whether he meant the similar command.
     */
    private static final double SUGGEST_COMMAND_THRESHOLD = 0.75;

    private final Set<CommandDescription> commands;

    /**
     * Create a command handler.
     *
     * @param commands The collection of available AuthMe commands
     */
    public CommandHandler(Set<CommandDescription> commands) {
        this.commands = commands;
    }

    /**
     * Map a command that was invoked to the proper {@link CommandDescription} or return a useful error
     * message upon failure.
     *
     * @param sender             The command sender (Bukkit).
     * @param bukkitCommandLabel The command label (Bukkit).
     * @param bukkitArgs         The command arguments (Bukkit).
     *
     * @return True if the command was executed, false otherwise.
     */
    public boolean processCommand(CommandSender sender, String bukkitCommandLabel, String[] bukkitArgs) {
        List<String> commandArgs = skipEmptyArguments(bukkitArgs);
        // Add the Bukkit command label to the front so we get a list like [authme, register, pass, passConfirm]
        commandArgs.add(0, bukkitCommandLabel);

        // TODO: remove commandParts
        CommandParts commandReference = new CommandParts(commandArgs);

        // Get a suitable command for this reference, and make sure it isn't null
        FoundCommandResult result = findCommand(commandReference);
        if (result == null) {
            // TODO ljacqu 20151204: Log more information to the console (bukkitCommandLabel)
            sender.sendMessage(ChatColor.DARK_RED + "Failed to parse " + AuthMe.getPluginName() + " command!");
            return false;
        }


        String baseCommand = commandArgs.get(0);

        // Make sure the difference between the command reference and the actual command isn't too big
        final double commandDifference = result.getDifference();
        if (commandDifference <= ASSUME_COMMAND_THRESHOLD) {

            // Show a message when the command handler is assuming a command
            if (commandDifference > 0) {
                sendCommandAssumptionMessage(sender, result, commandReference);
            }

            if (!result.hasPermission(sender)) {
                sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to use this command!");
            } else if (!result.hasProperArguments()) {
                sendImproperArgumentsMessage(sender, result, commandReference, baseCommand);
            } else {
                return result.executeCommand(sender);
            }
        } else {
            sendUnknownCommandMessage(sender, commandDifference, result, baseCommand);
        }
        return true;
    }

    /**
     * Skip all entries of the given array that are simply whitespace.
     *
     * @param args The array to process
     * @return List of the items that are not empty
     */
    private static List<String> skipEmptyArguments(String[] args) {
        List<String> cleanArguments = new ArrayList<>(args.length);
        for (String argument : args) {
            if (!StringUtils.isEmpty(argument)) {
                cleanArguments.add(argument);
            }
        }
        return cleanArguments;
    }


    private static CommandDescription mapToBase(String commandLabel) {
        for (CommandDescription command : CommandInitializer.getBaseCommands()) {
            if (command.getLabels().contains(commandLabel)) {
                return command;
            }
        }
        return null;
    }

    /**
     * Find the best suitable command for the specified reference.
     *
     * @param queryReference The query reference to find a command for.
     *
     * @return The command found, or null.
     */
    public FoundCommandResult findCommand(CommandParts queryReference) {
        // Make sure the command reference is valid
        if (queryReference.getCount() <= 0)
            return null;

        for (CommandDescription commandDescription : commands) {
            // Check whether there's a command description available for the
            // current command
            if (!commandDescription.isSuitableLabel(queryReference))
                continue;

            // Find the command reference, return the result
            return commandDescription.findCommand(queryReference);
        }

        // No applicable command description found, return false
        return null;
    }

    /**
     * Find the best suitable command for the specified reference.
     *
     * @param commandParts The query reference to find a command for.
     *
     * @return The command found, or null.
     */
    private CommandDescription findCommand(List<String> commandParts) {
        // Make sure the command reference is valid
        if (commandParts.isEmpty()) {
            return null;
        }

        // TODO ljacqu 20151129: Since we only use .contains() on the CommandDescription#labels after init, change
        // the type to set for faster lookup
        Iterable<CommandDescription> commandsToScan = CommandInitializer.getBaseCommands();
        CommandDescription result = null;
        for (String label : commandParts) {
            result = findLabel(label, commandsToScan);
            if (result == null) {
                return null;
            }
            commandsToScan = result.getChildren();
        }
        return result;
    }

    private static CommandDescription findLabel(String label, Iterable<CommandDescription> commands) {
        if (commands == null) {
            return null;
        }
        for (CommandDescription command : commands) {
            if (command.getLabels().contains(label)) { // TODO ljacqu should be case-insensitive
                return command;
            }
        }
        return null;
    }

    /**
     * Show an "unknown command" to the user and suggests an existing command if its similarity is within
     * the defined threshold.
     *
     * @param sender The command sender
     * @param commandDifference The difference between the invoked command and the existing one
     * @param result The command that was found during the mapping process
     * @param baseCommand The base command (TODO: This is probably already in FoundCommandResult)
     */
    private static void sendUnknownCommandMessage(CommandSender sender, double commandDifference,
                                                  FoundCommandResult result, String baseCommand) {
        CommandParts commandReference = result.getCommandReference();
        sender.sendMessage(ChatColor.DARK_RED + "Unknown command!");


        // Show a command suggestion if available and the difference isn't too big
        if (commandDifference < SUGGEST_COMMAND_THRESHOLD && result.getCommandDescription() != null) {
            sender.sendMessage(ChatColor.YELLOW + "Did you mean " + ChatColor.GOLD + "/"
                + result.getCommandDescription().getCommandReference(commandReference) + ChatColor.YELLOW + "?");
        }

        sender.sendMessage(ChatColor.YELLOW + "Use the command " + ChatColor.GOLD + "/" + baseCommand + " help"
            + ChatColor.YELLOW + " to view help.");
    }

    private static void sendImproperArgumentsMessage(CommandSender sender, FoundCommandResult result,
                                                     CommandParts commandReference, String baseCommand) {
        // Get the command and the suggested command reference
        CommandParts suggestedCommandReference =
            new CommandParts(result.getCommandDescription().getCommandReference(commandReference));
        CommandParts helpCommandReference = new CommandParts(suggestedCommandReference.getRange(1));

        // Show the invalid arguments warning
        sender.sendMessage(ChatColor.DARK_RED + "Incorrect command arguments!");

        // Show the command argument help
        HelpProvider.showHelp(sender, commandReference, suggestedCommandReference,
            true, false, true, false, false, false);

        // Show the command to use for detailed help
        sender.sendMessage(ChatColor.GOLD + "Detailed help: " + ChatColor.WHITE + "/" + baseCommand
            + " help " + helpCommandReference);
    }

    private static void sendCommandAssumptionMessage(CommandSender sender, FoundCommandResult result,
                                                     CommandParts commandReference) {
        CommandParts assumedCommandParts =
            new CommandParts(result.getCommandDescription().getCommandReference(commandReference));

        sender.sendMessage(ChatColor.DARK_RED + "Unknown command, assuming " + ChatColor.GOLD + "/"
            + assumedCommandParts + ChatColor.DARK_RED + "!");
    }
}
