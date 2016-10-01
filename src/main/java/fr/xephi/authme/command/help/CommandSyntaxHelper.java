package fr.xephi.authme.command.help;

import fr.xephi.authme.command.CommandArgumentDescription;
import fr.xephi.authme.command.CommandDescription;
import org.bukkit.ChatColor;

import java.util.List;

/**
 * Helper class for displaying the syntax of a command properly to a user.
 */
final class CommandSyntaxHelper {

    private CommandSyntaxHelper() {
    }

    public static String getSyntax(CommandDescription command, List<String> correctLabels) {
        String commandSyntax = ChatColor.WHITE + "/" + correctLabels.get(0) + ChatColor.YELLOW;
        for (int i = 1; i < correctLabels.size(); ++i) {
            commandSyntax += " " + correctLabels.get(i);
        }
        for (CommandArgumentDescription argument : command.getArguments()) {
            commandSyntax += " " + formatArgument(argument);
        }
        return commandSyntax;
    }

    /** Format a command argument with the proper type of brackets. */
    private static String formatArgument(CommandArgumentDescription argument) {
        if (argument.isOptional()) {
            return "[" + argument.getName() + "]";
        }
        return "<" + argument.getName() + ">";
    }

}
