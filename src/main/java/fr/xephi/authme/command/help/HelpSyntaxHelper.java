package fr.xephi.authme.command.help;

import fr.xephi.authme.command.CommandArgumentDescription;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.util.ListUtils;
import org.bukkit.ChatColor;

public class HelpSyntaxHelper {

    /**
     * Get the proper syntax for a command.
     *
     * @param commandDescription The command to get the syntax for.
     * @param commandReference The reference of the command.
     * @param alternativeLabel The alternative label to use for this command syntax.
     * @param highlight True to highlight the important parts of this command.
     *
     * @return The command with proper syntax.
     */
    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    public static String getCommandSyntax(CommandDescription commandDescription, CommandParts commandReference, String alternativeLabel, boolean highlight) {
        // Create a string builder to build the command
        StringBuilder sb = new StringBuilder();

        // Set the color and prefix a slash
        sb.append(ChatColor.WHITE + "/");

        // Get the help command reference, and the command label
        CommandParts helpCommandReference = commandDescription.getCommandReference(commandReference);
        final String parentCommand = (new CommandParts(helpCommandReference.getRange(0, helpCommandReference.getCount() - 1))).toString();
        String commandLabel = helpCommandReference.get(helpCommandReference.getCount() - 1);

        // Check whether the alternative label should be used
        if(alternativeLabel != null)
            if(alternativeLabel.trim().length() > 0)
                commandLabel = alternativeLabel;

        // Show the important bit of the command, highlight this part if required
        sb.append(ListUtils.implode(parentCommand, (highlight ? ChatColor.YELLOW + "" + ChatColor.BOLD : "") + commandLabel, " "));
        if(highlight)
            sb.append(ChatColor.YELLOW);

        // Add each command arguments
        for(CommandArgumentDescription arg : commandDescription.getArguments()) {
            // Add the argument as optional or non-optional argument
            if(!arg.isOptional())
                sb.append(ChatColor.ITALIC + " <" + arg.getLabel() + ">");
            else
                sb.append(ChatColor.ITALIC + " [" + arg.getLabel() + "]");
        }

        // Add some dots if the command allows unlimited arguments
        if(commandDescription.getMaximumArguments() < 0)
            sb.append(ChatColor.ITALIC + " ...");

        // Return the build command syntax
        return sb.toString();
    }
}
