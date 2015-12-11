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
                // Check perms + process
                break;
            case MISSING_BASE_COMMAND:
                sender.sendMessage(ChatColor.DARK_RED + "Failed to parse " + AuthMe.getPluginName() + " command!");
                return false;
            case INCORRECT_ARGUMENTS:
                // sendImproperArgumentsMessage(sender, result);
                break;
            case UNKNOWN_LABEL:
                // sendUnknownCommandMessage(sender);
                break;
            default:
                throw new RuntimeException("Unknown result '" + result.getResultStatus() + "'");
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


    /**
     * Show an "unknown command" to the user and suggests an existing command if its similarity is within
     * the defined threshold.
     *
     * @param sender The command sender
     * @param result The command that was found during the mapping process
     * @param baseCommand The base command
     */
    private static void sendUnknownCommandMessage(CommandSender sender, FoundCommandResult result, String baseCommand) {
        sender.sendMessage(ChatColor.DARK_RED + "Unknown command!");

        // Show a command suggestion if available and the difference isn't too big
        if (result.getDifference() < SUGGEST_COMMAND_THRESHOLD && result.getCommandDescription() != null) {
            sender.sendMessage(ChatColor.YELLOW + "Did you mean " + ChatColor.GOLD + "/"
                + result.getCommandDescription() + ChatColor.YELLOW + "?");
            // TODO: Define a proper string representation of command description
        }

        sender.sendMessage(ChatColor.YELLOW + "Use the command " + ChatColor.GOLD + "/" + baseCommand + " help"
            + ChatColor.YELLOW + " to view help.");
    }

    private static void sendImproperArgumentsMessage(CommandSender sender, FoundCommandResult result,
                                                     CommandParts commandReference, String baseCommand) {
        // Get the command and the suggested command reference
        // FIXME List<String> suggestedCommandReference =
        //    result.getCommandDescription().getCommandReference(commandReference).getList();
        // List<String> helpCommandReference = CollectionUtils.getRange(suggestedCommandReference, 1);

        // Show the invalid arguments warning
        sender.sendMessage(ChatColor.DARK_RED + "Incorrect command arguments!");

        // Show the command argument help
        // HelpProvider.showHelp(sender, commandReference, new CommandParts(suggestedCommandReference),
        //    true, false, true, false, false, false);

        // Show the command to use for detailed help
        // sender.sendMessage(ChatColor.GOLD + "Detailed help: " + ChatColor.WHITE + "/" + baseCommand
        //    + " help " + CommandUtils.labelsToString(helpCommandReference));
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
        List<String> remaining = parts.subList(1, parts.size());
        CommandDescription childCommand = returnSuitableChild(base, remaining);
        if (childCommand != null) {
            return new FoundCommandResult(childCommand, parts.subList(2, parts.size()), parts.subList(0, 2));
        } else if (isSuitableArgumentCount(base, remaining.size())) {
            return new FoundCommandResult(base, parts.subList(1, parts.size()), parts.subList(0, 1));
        }

        // TODO: return getCommandWithSmallestDifference()
        return null;

    }

    // TODO: Return FoundCommandDescription immediately
    private CommandDescription getCommandWithSmallestDifference(CommandDescription base, List<String> parts) {
        final String label = parts.get(0);
        final int argumentCount = parts.size() - 1;

        double minDifference = Double.POSITIVE_INFINITY;
        CommandDescription closestCommand = null;
        for (CommandDescription child : base.getChildren()) {
            double argumentDifference = getArgumentCountDifference(child, argumentCount);
            double labelDifference = getLabelDifference(child, label);
            // Weigh argument difference less
            double difference = labelDifference + argumentCount / 2;
            if (difference < minDifference) {
                minDifference = difference;
                closestCommand = child;
            }
        }
        return closestCommand;
    }

    private static boolean isSuitableArgumentCount(CommandDescription command, int argumentCount) {
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

    private static int getArgumentCountDifference(CommandDescription commandDescription, int givenArgumentsCount) {
        return Math.min(
            Math.abs(givenArgumentsCount - CommandUtils.getMinNumberOfArguments(commandDescription)),
            Math.abs(givenArgumentsCount - CommandUtils.getMaxNumberOfArguments(commandDescription)));
    }

    // Is the given command a suitable match for the given parts? parts is for example [changepassword, newpw, newpw]
    public CommandDescription returnSuitableChild(CommandDescription baseCommand, List<String> parts) {
        if (CollectionUtils.isEmpty(parts)) {
            return null;
        }

        final String label = parts.get(0).toLowerCase();
        final int argumentCount = parts.size() - 1;

        for (CommandDescription child : baseCommand.getChildren()) {
            if (child.hasLabel(label) && isSuitableArgumentCount(child, argumentCount)) {
                return child;
            }
        }
        return null;
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

}
