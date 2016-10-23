package fr.xephi.authme.command;

import com.google.common.collect.Lists;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Constructs a hierarchical list of commands for the given command. The commands are in order:
     * the parents of the given command precede the provided command. For example, given the command
     * for {@code /authme register}, a list with {@code [{authme}, {authme register}]} is returned.
     *
     * @param command the command to build a parent list for
     * @return the parent list
     */
    public static List<CommandDescription> constructParentList(CommandDescription command) {
        List<CommandDescription> commands = new ArrayList<>();
        CommandDescription currentCommand = command;
        while (currentCommand != null) {
            commands.add(currentCommand);
            currentCommand = currentCommand.getParent();
        }
        return Lists.reverse(commands);
    }

    public static String buildSyntax(CommandDescription command) {
        String arguments = command.getArguments().stream()
            .map(arg -> formatArgument(arg))
            .collect(Collectors.joining(" "));
        return (constructCommandPath(command) + " " + arguments).trim();
    }

    public static String buildSyntax(CommandDescription command, List<String> correctLabels) {
        String commandSyntax = ChatColor.WHITE + "/" + correctLabels.get(0) + ChatColor.YELLOW;
        for (int i = 1; i < correctLabels.size(); ++i) {
            commandSyntax += " " + correctLabels.get(i);
        }
        for (CommandArgumentDescription argument : command.getArguments()) {
            commandSyntax += " " + formatArgument(argument);
        }
        return commandSyntax;
    }

    /**
     * Format a command argument with the proper type of brackets.
     *
     * @param argument the argument to format
     * @return the formatted argument
     */
    public static String formatArgument(CommandArgumentDescription argument) {
        if (argument.isOptional()) {
            return "[" + argument.getName() + "]";
        }
        return "<" + argument.getName() + ">";
    }
}
