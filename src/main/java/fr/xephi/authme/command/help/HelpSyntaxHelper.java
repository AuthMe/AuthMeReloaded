package fr.xephi.authme.command.help;

import fr.xephi.authme.command.CommandArgumentDescription;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandUtils;
import fr.xephi.authme.util.CollectionUtils;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.ChatColor;

import java.util.List;

/**
 * Helper class for formatting a command's structure (name and arguments)
 * for a Minecraft user.
 */
public final class HelpSyntaxHelper {

    private HelpSyntaxHelper() {
        // Helper class
    }

    /**
     * Get the formatted syntax for a command.
     *
     * @param commandDescription The command to build the syntax for.
     * @param commandReference   The reference of the command.
     * @param alternativeLabel   The alternative label to use for this command syntax.
     * @param highlight          True to highlight the important parts of this command.
     *
     * @return The command with proper syntax.
     */
    public static String getCommandSyntax(CommandDescription commandDescription, CommandParts commandReference,
                                          String alternativeLabel, boolean highlight) {
        // Create a string builder with white color and prefixed slash
        StringBuilder sb = new StringBuilder()
            .append(ChatColor.WHITE)
            .append("/");

        // Get the help command reference, and the command label
        CommandParts helpCommandReference = commandDescription.getCommandReference(commandReference);
        List<String> helpLabels = helpCommandReference.getList();

        final String parentCommand = CommandUtils.labelsToString(
            CollectionUtils.getRange(helpCommandReference.getList(), 0, helpLabels.size() - 1));

        // Check whether the alternative label should be used
        String commandLabel;
        if (StringUtils.isEmpty(alternativeLabel)) {
            commandLabel = helpLabels.get(helpLabels.size() - 1);
        } else {
            commandLabel = alternativeLabel;
        }

        // Show the important bit of the command, highlight this part if required
        sb.append(parentCommand)
            .append(" ")
            .append(highlight ? ChatColor.YELLOW.toString() + ChatColor.BOLD : "")
            .append(commandLabel);

        if (highlight) {
            sb.append(ChatColor.YELLOW);
        }

        // Add each command argument
        for (CommandArgumentDescription arg : commandDescription.getArguments()) {
            sb.append(ChatColor.ITALIC).append(formatArgument(arg));
        }

        // Return the build command syntax
        return sb.toString();
    }

    private static String formatArgument(CommandArgumentDescription argument) {
        if (argument.isOptional()) {
            return " [" + argument.getName() + "]";
        }
        return " <" + argument.getName() + ">";
    }
}
