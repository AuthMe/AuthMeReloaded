package fr.xephi.authme.command;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.executable.HelpCommand;
import fr.xephi.authme.command.help.HelpProvider;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.util.CollectionUtils;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static fr.xephi.authme.command.FoundResultStatus.INCORRECT_ARGUMENTS;
import static fr.xephi.authme.command.FoundResultStatus.MISSING_BASE_COMMAND;
import static fr.xephi.authme.command.FoundResultStatus.UNKNOWN_LABEL;

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

    private static final Class<? extends ExecutableCommand> HELP_COMMAND_CLASS = HelpCommand.class;

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

    /**
     * Check a command's permissions and execute it with the given arguments if the check succeeds.
     *
     * @param sender The command sender
     * @param command The command to process
     * @param arguments The arguments to pass to the command
     */
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
            sender.sendMessage(ChatColor.YELLOW + "Did you mean " + ChatColor.GOLD
                + CommandUtils.constructCommandPath(result.getCommandDescription()) + ChatColor.YELLOW + "?");
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
        List<String> lines = HelpProvider.printHelp(result, HelpProvider.SHOW_ARGUMENTS);
        for (String line : lines) {
            sender.sendMessage(line);
        }

        List<String> labels = result.getLabels();
        String childLabel = labels.size() >= 2 ? labels.get(1) : "";
        sender.sendMessage(ChatColor.GOLD + "Detailed help: " + ChatColor.WHITE
            + "/" + labels.get(0) + " help " + childLabel);
    }

    // TODO ljacqu 20151212: Remove me once I am a MessageKey
    private void sendPermissionDeniedError(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to use this command!");
    }

    /**
     * Map incoming command parts to a command. This processes all parts and distinguishes the labels from arguments.
     *
     * @param parts The parts to map to commands and arguments
     * @return The generated {@link FoundCommandResult}
     */
    public FoundCommandResult mapPartsToCommand(final List<String> parts) {
        if (CollectionUtils.isEmpty(parts)) {
            return new FoundCommandResult(null, parts, null, 0.0, MISSING_BASE_COMMAND);
        }

        CommandDescription base = getBaseCommand(parts.get(0));
        if (base == null) {
            return new FoundCommandResult(null, parts, null, 0.0, MISSING_BASE_COMMAND);
        }

        // Prefer labels: /register help goes to "Help command", not "Register command" with argument 'help'
        List<String> remainingParts = parts.subList(1, parts.size());
        CommandDescription childCommand = getSuitableChild(base, remainingParts);
        if (childCommand != null) {
            FoundCommandResult result = new FoundCommandResult(
                childCommand, parts.subList(0, 2), parts.subList(2, parts.size()));
            transformResultForHelp(result);
            return result;
        } else if (hasSuitableArgumentCount(base, remainingParts.size())) {
            return new FoundCommandResult(base, parts.subList(0, 1), parts.subList(1, parts.size()));
        }

        return getCommandWithSmallestDifference(base, parts);
    }

    private FoundCommandResult getCommandWithSmallestDifference(CommandDescription base, List<String> parts) {
        final String childLabel = parts.size() >= 2 ? parts.get(1) : null;
        double minDifference = Double.POSITIVE_INFINITY;
        CommandDescription closestCommand = null;

        if (childLabel != null) {
            for (CommandDescription child : base.getChildren()) {
                double difference = getLabelDifference(child, childLabel);
                if (difference < minDifference) {
                    minDifference = difference;
                    closestCommand = child;
                }
            }
        }

        // base command may have no children or no child label was present
        if (closestCommand == null) {
            return new FoundCommandResult(null, parts, null, minDifference, UNKNOWN_LABEL);
        }

        FoundResultStatus status = (minDifference == 0.0) ? INCORRECT_ARGUMENTS : UNKNOWN_LABEL;
        final int partsSize = parts.size();
        List<String> labels = parts.subList(0, Math.min(closestCommand.getParentCount() + 1, partsSize));
        List<String> arguments = (labels.size() == partsSize)
            ? new ArrayList<String>()
            : parts.subList(labels.size(), partsSize);

        return new FoundCommandResult(closestCommand, labels, arguments, minDifference, status);
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

    private static void transformResultForHelp(FoundCommandResult result) {
        if (result.getCommandDescription() != null
            && HELP_COMMAND_CLASS.equals(result.getCommandDescription().getExecutableCommand().getClass())) {
            // For "/authme help register" we have labels = [authme, help] and arguments = [register]
            // But for the help command we want labels = [authme, help] and arguments = [authme, register],
            // so we can use the arguments as the labels to the command to show help for
            final String baseLabel = result.getLabels().get(0);
            result.getArguments().add(0, baseLabel);
        }
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
