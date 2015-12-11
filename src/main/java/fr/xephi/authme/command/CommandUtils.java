package fr.xephi.authme.command;

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

}
