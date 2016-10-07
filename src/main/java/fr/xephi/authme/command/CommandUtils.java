package fr.xephi.authme.command;

import com.google.common.collect.Lists;

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
