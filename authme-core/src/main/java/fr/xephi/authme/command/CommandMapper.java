package fr.xephi.authme.command;

import fr.xephi.authme.command.executable.HelpCommand;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.util.StringUtils;
import fr.xephi.authme.util.Utils;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static fr.xephi.authme.command.FoundResultStatus.INCORRECT_ARGUMENTS;
import static fr.xephi.authme.command.FoundResultStatus.MISSING_BASE_COMMAND;
import static fr.xephi.authme.command.FoundResultStatus.UNKNOWN_LABEL;

/**
 * Maps incoming command parts to the correct {@link CommandDescription}.
 */
public class CommandMapper {

    /**
     * The class of the help command, to which the base label should also be passed in the arguments.
     */
    private static final Class<? extends ExecutableCommand> HELP_COMMAND_CLASS = HelpCommand.class;

    private final Collection<CommandDescription> baseCommands;
    private final PermissionsManager permissionsManager;

    @Inject
    public CommandMapper(CommandInitializer commandInitializer, PermissionsManager permissionsManager) {
        this.baseCommands = commandInitializer.getCommands();
        this.permissionsManager = permissionsManager;
    }


    /**
     * Map incoming command parts to a command. This processes all parts and distinguishes the labels from arguments.
     *
     * @param sender The command sender (null if none applicable)
     * @param parts The parts to map to commands and arguments
     * @return The generated {@link FoundCommandResult}
     */
    public FoundCommandResult mapPartsToCommand(CommandSender sender, List<String> parts) {
        if (Utils.isCollectionEmpty(parts)) {
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
            FoundResultStatus status = getPermissionAwareStatus(sender, childCommand);
            FoundCommandResult result = new FoundCommandResult(
                childCommand, parts.subList(0, 2), parts.subList(2, parts.size()), 0.0, status);
            return transformResultForHelp(result);
        } else if (hasSuitableArgumentCount(base, remainingParts.size())) {
            FoundResultStatus status = getPermissionAwareStatus(sender, base);
            return new FoundCommandResult(base, parts.subList(0, 1), parts.subList(1, parts.size()), 0.0, status);
        }

        return getCommandWithSmallestDifference(base, parts);
    }

    /**
     * Return all {@link ExecutableCommand} classes referenced in {@link CommandDescription} objects.
     *
     * @return all classes
     * @see CommandInitializer#getCommands
     */
    public Set<Class<? extends ExecutableCommand>> getCommandClasses() {
        Set<Class<? extends ExecutableCommand>> classes = new HashSet<>(50);
        for (CommandDescription command : baseCommands) {
            classes.add(command.getExecutableCommand());
            for (CommandDescription child : command.getChildren()) {
                classes.add(child.getExecutableCommand());
            }
        }
        return classes;
    }

    /**
     * Return the command whose label matches the given parts the best. This method is called when
     * a successful mapping could not be performed.
     *
     * @param base the base command
     * @param parts the command parts
     * @return the closest result
     */
    private static FoundCommandResult getCommandWithSmallestDifference(CommandDescription base, List<String> parts) {
        // Return the base command with incorrect arg count error if we only have one part
        if (parts.size() <= 1) {
            return new FoundCommandResult(base, parts, new ArrayList<>(), 0.0, INCORRECT_ARGUMENTS);
        }

        final String childLabel = parts.get(1);
        double minDifference = Double.POSITIVE_INFINITY;
        CommandDescription closestCommand = null;

        for (CommandDescription child : base.getChildren()) {
            double difference = getLabelDifference(child, childLabel);
            if (difference < minDifference) {
                minDifference = difference;
                closestCommand = child;
            }
        }

        // base command may have no children, in which case we return the base command with incorrect arguments error
        if (closestCommand == null) {
            return new FoundCommandResult(
                base, parts.subList(0, 1), parts.subList(1, parts.size()), 0.0, INCORRECT_ARGUMENTS);
        }

        FoundResultStatus status = (minDifference == 0.0) ? INCORRECT_ARGUMENTS : UNKNOWN_LABEL;
        final int partsSize = parts.size();
        List<String> labels = parts.subList(0, Math.min(closestCommand.getLabelCount(), partsSize));
        List<String> arguments = (labels.size() == partsSize)
            ? new ArrayList<>()
            : parts.subList(labels.size(), partsSize);

        return new FoundCommandResult(closestCommand, labels, arguments, minDifference, status);
    }

    private CommandDescription getBaseCommand(String label) {
        String baseLabel = label.toLowerCase(Locale.ROOT);
        if (baseLabel.startsWith("authme:")) {
            baseLabel = baseLabel.substring("authme:".length());
        }
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
    private static CommandDescription getSuitableChild(CommandDescription baseCommand, List<String> parts) {
        if (Utils.isCollectionEmpty(parts)) {
            return null;
        }

        final String label = parts.get(0).toLowerCase(Locale.ROOT);
        final int argumentCount = parts.size() - 1;

        for (CommandDescription child : baseCommand.getChildren()) {
            if (child.hasLabel(label) && hasSuitableArgumentCount(child, argumentCount)) {
                return child;
            }
        }
        return null;
    }

    private static FoundCommandResult transformResultForHelp(FoundCommandResult result) {
        if (result.getCommandDescription() != null
            && HELP_COMMAND_CLASS == result.getCommandDescription().getExecutableCommand()) {
            // For "/authme help register" we have labels = [authme, help] and arguments = [register]
            // But for the help command we want labels = [authme, help] and arguments = [authme, register],
            // so we can use the arguments as the labels to the command to show help for
            List<String> arguments = new ArrayList<>(result.getArguments());
            arguments.add(0, result.getLabels().get(0));
            return new FoundCommandResult(result.getCommandDescription(), result.getLabels(),
                arguments, result.getDifference(), result.getResultStatus());
        }
        return result;
    }

    private FoundResultStatus getPermissionAwareStatus(CommandSender sender, CommandDescription command) {
        if (sender != null && !permissionsManager.hasPermission(sender, command.getPermission())) {
            return FoundResultStatus.NO_PERMISSION;
        }
        return FoundResultStatus.SUCCESS;
    }

    private static boolean hasSuitableArgumentCount(CommandDescription command, int argumentCount) {
        int minArgs = CommandUtils.getMinNumberOfArguments(command);
        int maxArgs = CommandUtils.getMaxNumberOfArguments(command);

        return argumentCount >= minArgs && argumentCount <= maxArgs;
    }

    private static double getLabelDifference(CommandDescription command, String givenLabel) {
        return command.getLabels().stream()
            .map(label -> StringUtils.getDifference(label, givenLabel))
            .min(Double::compareTo)
            .orElseThrow(() -> new IllegalStateException("Command does not have any labels set"));
    }

}
