package fr.xephi.authme.command;

import com.google.common.collect.Lists;
import fr.xephi.authme.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class CommandUtils {

    private CommandUtils() {
    }

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
     * the items ["authme", "register", "player"] will return "authme register player".
     *
     * @param labels The labels to format
     *
     * @return The space-separated labels
     */
    public static String labelsToString(Iterable<String> labels) {
        return StringUtils.join(" ", labels);
    }

    public static String constructCommandPath(CommandDescription command) {
        StringBuilder sb = new StringBuilder();
        String prefix = "/";
        for (CommandDescription ancestor : constructParentList(command)) {
            sb.append(prefix).append(ancestor.getLabels().get(0));
            prefix = " ";
        }
        return sb.toString();
    }

    public static List<CommandDescription> constructParentList(CommandDescription command) {
        List<CommandDescription> commands = new ArrayList<>();
        CommandDescription currentCommand = command;
        while (currentCommand != null) {
            commands.add(currentCommand);
            currentCommand = currentCommand.getParent();
        }
        return Lists.reverse(commands);
    }
}
