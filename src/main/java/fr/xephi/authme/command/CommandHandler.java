package fr.xephi.authme.command;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.help.HelpProvider;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Process a command.
     *
     * @param sender             The command sender (Bukkit).
     * @param bukkitCommand      The command (Bukkit).
     * @param bukkitCommandLabel The command label (Bukkit).
     * @param bukkitArgs         The command arguments (Bukkit).
     *
     * @return True if the command was executed, false otherwise.
     */
    // TODO ljacqu 20151129: Rename onCommand() method to something not suggesting it is auto-invoked by an event
    public boolean onCommand(CommandSender sender, Command bukkitCommand, String bukkitCommandLabel, String[] bukkitArgs) {
        List<String> commandArgs = skipEmptyArguments(bukkitArgs);

        // Make sure the command isn't empty (does this happen?)
        CommandParts commandReference = new CommandParts(bukkitCommandLabel, commandArgs);
        if (commandReference.getCount() == 0)
            return false;

        // Get a suitable command for this reference, and make sure it isn't null
        FoundCommandResult result = findCommand(commandReference);
        if (result == null) {
            sender.sendMessage(ChatColor.DARK_RED + "Failed to parse " + AuthMe.getPluginName() + " command!");
            return false;
        }

        // Get the base command
        String baseCommand = commandReference.get(0);

        // Make sure the difference between the command reference and the actual command isn't too big
        final double commandDifference = result.getDifference();
        if (commandDifference > ASSUME_COMMAND_THRESHOLD) {
            // Show the unknown command warning
            sender.sendMessage(ChatColor.DARK_RED + "Unknown command!");

            // Show a command suggestion if available and the difference isn't too big
            if (commandDifference < SUGGEST_COMMAND_THRESHOLD)
                if (result.getCommandDescription() != null)
                    sender.sendMessage(ChatColor.YELLOW + "Did you mean " + ChatColor.GOLD + "/" + result.getCommandDescription().getCommandReference(commandReference) + ChatColor.YELLOW + "?");

            // Show the help command
            sender.sendMessage(ChatColor.YELLOW + "Use the command " + ChatColor.GOLD + "/" + baseCommand + " help" + ChatColor.YELLOW + " to view help.");
            return true;
        }

        // Show a message when the command handler is assuming a command
        if (commandDifference > 0) {
            // Get the suggested command
            CommandParts suggestedCommandParts = new CommandParts(result.getCommandDescription().getCommandReference(commandReference));

            // Show the suggested command
            sender.sendMessage(ChatColor.DARK_RED + "Unknown command, assuming " + ChatColor.GOLD + "/" + suggestedCommandParts +
                ChatColor.DARK_RED + "!");
        }

        // Make sure the command is executable
        if (!result.isExecutable()) {
            // Get the command reference
            CommandParts helpCommandReference = new CommandParts(result.getCommandReference().getRange(1));

            // Show the unknown command warning
            sender.sendMessage(ChatColor.DARK_RED + "Invalid command!");

            // Show the help command
            sender.sendMessage(ChatColor.YELLOW + "Use the command " + ChatColor.GOLD + "/" + baseCommand + " help " + helpCommandReference + ChatColor.YELLOW + " to view help.");
            return true;
        }

        // Make sure the command sender has permission
        if (!result.hasPermission(sender)) {
            // Show the no permissions warning
            sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to use this command!");
            return true;
        }

        // Make sure the command sender has permission
        if (!result.hasProperArguments()) {
            // Get the command and the suggested command reference
            CommandParts suggestedCommandReference = new CommandParts(result.getCommandDescription().getCommandReference(commandReference));
            CommandParts helpCommandReference = new CommandParts(suggestedCommandReference.getRange(1));

            // Show the invalid arguments warning
            sender.sendMessage(ChatColor.DARK_RED + "Incorrect command arguments!");

            // Show the command argument help
            HelpProvider.showHelp(sender, commandReference, suggestedCommandReference, true, false, true, false, false, false);

            // Show the command to use for detailed help
            sender.sendMessage(ChatColor.GOLD + "Detailed help: " + ChatColor.WHITE + "/" + baseCommand + " help " + helpCommandReference);
            return true;
        }

        // Execute the command if it's suitable
        return result.executeCommand(sender);
    }

    /**
     * Skips all entries of the given array that are simply whitespace.
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

        // TODO ljacqu 20151129: If base commands are only used in here (or in the future CommandHandler after changes),
        // it might make sense to make the CommandInitializer package-private and to return its result into this class
        // instead of regularly fetching the list of base commands from the other class.
        for (CommandDescription commandDescription : CommandInitializer.getBaseCommands()) {
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
    public CommandDescription findCommand(List<String> commandParts) {
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
            if (command.getLabels().contains(label)) {
                return command;
            }
        }
        return null;
    }
}
