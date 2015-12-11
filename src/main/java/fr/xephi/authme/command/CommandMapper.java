package fr.xephi.authme.command;

import fr.xephi.authme.util.CollectionUtils;
import fr.xephi.authme.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Class responsible for mapping incoming arguments to a {@link CommandDescription}.
 */
public class CommandMapper {

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
     * Map incoming command parts to an actual command.
     *
     * @param parts The parts to process
     * @return The generated result
     */
    public FoundCommandResult mapPartsToCommand(final List<String> parts) {
        if (CollectionUtils.isEmpty(parts)) {
            return null; // TODO pass on the information that the base could not be mapped
        }

        CommandDescription base = getBaseCommand(parts.get(0));
        if (base == null) {
            return null; // TODO Pass on the information that base could not be mapped
        }

        List<String> remaining = parts.subList(1, parts.size());

        // Prefer labels: /register help goes to "Help command", not "Register command" with argument 'help'
        CommandDescription childCommand = returnSuitableChild(base, remaining);
        if (childCommand != null) {
            // return childcommand: it's valid...
        } else if (isSuitableArgumentCount(base, remaining.size())) {
            // return base... it's valid
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
            if (child.getLabels().contains(label) && isSuitableArgumentCount(child, argumentCount)) {
                return child;
            }
        }
        return null;
    }

    public CommandDescription getBaseCommand(String label) {
        String baseLabel = label.toLowerCase();
        for (CommandDescription command : CommandInitializer.getBaseCommands()) {
            if (command.getLabels().contains(baseLabel)) {
                return command;
            }
        }
        return null;
    }

}
