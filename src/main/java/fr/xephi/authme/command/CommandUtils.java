package fr.xephi.authme.command;

import java.util.List;

import fr.xephi.authme.util.CollectionUtils;
import fr.xephi.authme.util.StringUtils;

public final class CommandUtils {

    public static int getMinNumberOfArguments(CommandDescription command) {
        int mandatoryArguments = 0;
        for (CommandArgumentDescription argument : command.getArguments()) {
            if (!argument.isOptional()) {
                ++mandatoryArguments;
            }
        }
        return mandatoryArguments;
    }

    public static int getMaxNumberOfArguments(CommandDescription command) {
        return command.getArguments().size();
    }

    /**
     * Provide a textual representation of a list of labels to show it as a command. For example, a list containing
     * the items ["authme", "register", "player"] it will return "authme register player".
     *
     * @param labels The labels to format
     * @return The space-separated labels
     */
    public static String labelsToString(Iterable<String> labels) {
        return StringUtils.join(" ", labels);
    }

    public static double getDifference(List<String> labels1, List<String> labels2, boolean fullCompare) {
        // Make sure the other reference is correct
        if (labels1 == null || labels2 == null) {
            return -1;
        }

        // Get the range to use
        int range = Math.min(labels1.size(), labels2.size());

        // Get and the difference
        if (fullCompare) {
            return StringUtils.getDifference(CommandUtils.labelsToString(labels1), CommandUtils.labelsToString(labels2));
        }
        return StringUtils.getDifference(
            labelsToString(CollectionUtils.getRange(labels1, range - 1, 1)),
            labelsToString(CollectionUtils.getRange(labels2, range - 1, 1)));
    }





}
