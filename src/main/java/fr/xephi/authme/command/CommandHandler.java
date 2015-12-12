package fr.xephi.authme.command;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.help.HelpProvider;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.util.CollectionUtils;
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
     * The threshold for suggesting a similar command. If the difference is below this value, we will
     * ask the player whether he meant the similar command.
     */
    private static final double SUGGEST_COMMAND_THRESHOLD = 0.75;

    private final Set<CommandDescription> baseCommands;
    private final PermissionsManager permissionsManager;

    /**
     * Create a command handler.
     *
     * @param baseCommands The collection of available AuthMe base commands
     */
    public CommandHandler(Set<CommandDescription> baseCommands, PermissionsManager permissionsManager) {
        this.baseCommands = baseCommands;
        this.permissionsManager = permissionsManager;
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
        // Add the Bukkit command label to the front so we get a list like [authme, register, bobby, mysecret]
        List<String> parts = skipEmptyArguments(bukkitArgs);
        parts.add(0, bukkitCommandLabel);

        // Get the base command of the result, e.g. authme for [authme, register, bobby, mysecret]
        FoundCommandResult result = mapPartsToCommand(parts);
        switch (result.getResultStatus()) {
            case SUCCESS:
                executeCommandIfAllowed(sender, result.getCommandDescription(), result.getArguments());
                break;
            case MISSING_BASE_COMMAND:
                sender.sendMessage(ChatColor.DARK_RED + "Failed to parse " + AuthMe.getPluginName() + " command!");
                return false;
            case INCORRECT_ARGUMENTS:
                sendImproperArgumentsMessage(sender, result);
                break;
            case UNKNOWN_LABEL:
                sendUnknownCommandMessage(sender, result);
                break;
            default:
                throw new RuntimeException("Unknown result '" + result.getResultStatus() + "'");
        }

        return true;
    }

    private void executeCommandIfAllowed(CommandSender sender, CommandDescription command, List<String> arguments) {
        if (permissionsManager.hasPermission(sender, command)) {
            command.getExecutableCommand().executeCommand(sender, arguments);
        } else {
            sendPermissionDeniedError(sender);
        }
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

    /**
     * Show an "unknown command" to the user and suggests an existing command if its similarity is within
     * the defined threshold.
     *
     * @param sender The command sender
     * @param result The command that was found during the mapping process
     */
    private static void sendUnknownCommandMessage(CommandSender sender, FoundCommandResult result) {
        sender.sendMessage(ChatColor.DARK_RED + "Unknown command!");

        // Show a command suggestion if available and the difference isn't too big
        if (result.getDifference() < SUGGEST_COMMAND_THRESHOLD && result.getCommandDescription() != null) {
            sender.sendMessage(ChatColor.YELLOW + "Did you mean " + ChatColor.GOLD + "/"
                + result.getCommandDescription() + ChatColor.YELLOW + "?");
            // TODO: Define a proper string representation of command description
        }

        sender.sendMessage(ChatColor.YELLOW + "Use the command " + ChatColor.GOLD + "/" + result.getLabels().get(0)
            + " help" + ChatColor.YELLOW + " to view help.");
    }

    private void sendImproperArgumentsMessage(CommandSender sender, FoundCommandResult result) {
        CommandDescription command = result.getCommandDescription();
        if (!permissionsManager.hasPermission(sender, command)) {
            sendPermissionDeniedError(sender);
            return;
        }

        // Show the command argument help
        sender.sendMessage(ChatColor.DARK_RED + "Incorrect command arguments!");
        // TODO: Define showHelp(CommandSender, CommandDescription, List<String>, boolean, boolean, ...)
        List<String> labels = result.getLabels();
        HelpProvider.showHelp(sender, command, labels, true, false, true, false, false, false);
        sender.sendMessage(ChatColor.GOLD + "Detailed help: " + ChatColor.WHITE + "/" + labels.get(0)
            + " help " + CommandUtils.labelsToString(labels.subList(1, labels.size())));
    }

    // TODO ljacqu 20151212: Remove me once I am a MessageKey
    private void sendPermissionDeniedError(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to use this command!");
    }

    public FoundCommandResult mapPartsToCommand(final List<String> parts) {
        if (CollectionUtils.isEmpty(parts)) {
            return new FoundCommandResult(null, parts, null, 0.0, FoundCommandResult.ResultStatus.MISSING_BASE_COMMAND);
        }

        CommandDescription base = getBaseCommand(parts.get(0));
        if (base == null) {
            return new FoundCommandResult(null, parts, null, 0.0, FoundCommandResult.ResultStatus.MISSING_BASE_COMMAND);
        }

        // Prefer labels: /register help goes to "Help command", not "Register command" with argument 'help'
        List<String> remainingParts = parts.subList(1, parts.size());
        CommandDescription childCommand = getSuitableChild(base, remainingParts);
        if (childCommand != null) {
            return new FoundCommandResult(childCommand, parts.subList(2, parts.size()), parts.subList(0, 2));
        } else if (hasSuitableArgumentCount(base, remainingParts.size())) {
            return new FoundCommandResult(base, parts.subList(1, parts.size()), parts.subList(0, 1));
        }

        return getCommandWithSmallestDifference(base, parts);
    }

    private FoundCommandResult getCommandWithSmallestDifference(CommandDescription base, List<String> parts) {
        final String label = parts.get(0);

        double minDifference = Double.POSITIVE_INFINITY;
        CommandDescription closestCommand = null;
        for (CommandDescription child : base.getChildren()) {
            double difference = getLabelDifference(child, label);
            if (difference < minDifference) {
                minDifference = difference;
                closestCommand = child;
            }
        }
        // TODO: Return the full list of labels and arguments
        return new FoundCommandResult(
            closestCommand, null, null, minDifference, FoundCommandResult.ResultStatus.UNKNOWN_LABEL);
    }

    private CommandDescription getBaseCommand(String label) {
        String baseLabel = label.toLowerCase();
        for (CommandDescription command : baseCommands) {
            if (command.hasLabel(baseLabel)) {
                return command;
            }
        }
        return null;
    }

    /**
     * Return a child from a base command if the label and the argument count match.
     *
     * @param baseCommand The base command whose children should be checked
     * @param parts The command parts received from the invocation; the first item is the potential label and any
     *              other items are command arguments. The first initial part that led to the base command should not
     *              be present.
     *
     * @return A command if there was a complete match (including proper argument count), null otherwise
     */
    private CommandDescription getSuitableChild(CommandDescription baseCommand, List<String> parts) {
        if (CollectionUtils.isEmpty(parts)) {
            return null;
        }

        final String label = parts.get(0).toLowerCase();
        final int argumentCount = parts.size() - 1;

        for (CommandDescription child : baseCommand.getChildren()) {
            if (child.hasLabel(label) && hasSuitableArgumentCount(child, argumentCount)) {
                return child;
            }
        }
        return null;
    }

    private static boolean hasSuitableArgumentCount(CommandDescription command, int argumentCount) {
        int minArgs = CommandUtils.getMinNumberOfArguments(command);
        int maxArgs = CommandUtils.getMaxNumberOfArguments(command);

        return argumentCount >= minArgs && argumentCount <= maxArgs;
    }

    private static double getLabelDifference(CommandDescription command, String givenLabel) {
        double minDifference = Double.POSITIVE_INFINITY;
        for (String commandLabel : command.getLabels()) {
            double difference = StringUtils.getDifference(commandLabel, givenLabel);
            if (difference < minDifference) {
                minDifference = difference;
            }
        }
        return minDifference;
    }

}
