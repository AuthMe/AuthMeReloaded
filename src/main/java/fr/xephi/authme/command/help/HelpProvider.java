package fr.xephi.authme.command.help;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.CommandUtils;
import fr.xephi.authme.command.FoundCommandResult;
import fr.xephi.authme.settings.Settings;

import fr.xephi.authme.util.CollectionUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class HelpProvider {

    /**
     * Show help for a specific command.
     *
     * @param sender    The command sender the help needs to be shown to.
     * @param reference The command reference to the help command.
     * @param helpQuery The query to show help for.
     */
    public static void showHelp(CommandSender sender, CommandParts reference, CommandParts helpQuery) {
        showHelp(sender, reference, helpQuery, true, true, true, true, true, true);
    }

    /**
     * Show help for a specific command.
     *
     * @param sender           The command sender the help needs to be shown to.
     * @param reference        The command reference to the help command.
     * @param helpQuery        The query to show help for.
     * @param showCommand      True to show the command.
     * @param showDescription  True to show the command description, both the short and detailed description.
     * @param showArguments    True to show the command argument help.
     * @param showPermissions  True to show the command permission help.
     * @param showAlternatives True to show the command alternatives.
     * @param showCommands     True to show the child commands.
     */
    public static void showHelp(CommandSender sender, CommandParts reference, CommandParts helpQuery, boolean showCommand, boolean showDescription, boolean showArguments, boolean showPermissions, boolean showAlternatives, boolean showCommands) {
        // Find the command for this help query, one with and one without a prefixed base command
        FoundCommandResult result = AuthMe.getInstance().getCommandHandler().findCommand(new CommandParts(helpQuery.getList()));

        // TODO ljacqu 20151204 Fix me to nicer code
        List<String> parts = new ArrayList<>(helpQuery.getList());
        parts.add(0, reference.get(0));
        CommandParts commandReferenceOther = new CommandParts(parts);

        FoundCommandResult resultOther = AuthMe.getInstance().getCommandHandler().findCommand(commandReferenceOther);
        if (resultOther != null) {
            if (result == null)
                result = resultOther;

            else if (result.getDifference() > resultOther.getDifference())
                result = resultOther;
        }

        // Make sure a result was found
        if (result == null) {
            // Show a warning message
            sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.ITALIC + helpQuery);
            sender.sendMessage(ChatColor.DARK_RED + "Couldn't show any help information for this help query.");
            return;
        }

        // Get the command description, and make sure it's valid
        CommandDescription command = result.getCommandDescription();
        if (command == null) {
            // Show a warning message
            sender.sendMessage(ChatColor.DARK_RED + "Failed to retrieve any help information!");
            return;
        }

        // Get the proper command reference to use for the help page
        CommandParts commandReference = command.getCommandReference(result.getQueryReference());

        // Get the base command
        String baseCommand = commandReference.get(0);

        // Make sure the difference between the command reference and the actual command isn't too big
        final double commandDifference = result.getDifference();
        if (commandDifference > 0.20) {
            // Show the unknown command warning
            sender.sendMessage(ChatColor.DARK_RED + "No help found for '" + helpQuery + "'!");

            // Show a command suggestion if available and the difference isn't too big
            if (commandDifference < 0.75 && result.getCommandDescription() != null) {
                // Get the suggested command
                List<String> suggestedCommandParts = CollectionUtils.getRange(
                    result.getCommandDescription().getCommandReference(commandReference).getList(), 1);
                sender.sendMessage(ChatColor.YELLOW + "Did you mean " + ChatColor.GOLD + "/" + baseCommand
                    + " help " + CommandUtils.labelsToString(suggestedCommandParts) + ChatColor.YELLOW + "?");
            }

            // Show the help command
            sender.sendMessage(ChatColor.YELLOW + "Use the command " + ChatColor.GOLD + "/" + baseCommand + " help" + ChatColor.YELLOW + " to view help.");
            return;
        }

        // Show a message when the command handler is assuming a command
        if (commandDifference > 0) {
            // Get the suggested command
            List<String> suggestedCommandParts = CollectionUtils.getRange(
                result.getCommandDescription().getCommandReference(commandReference).getList(), 1);

            // Show the suggested command
            sender.sendMessage(ChatColor.DARK_RED + "No help found, assuming '" + ChatColor.GOLD
                + CommandUtils.labelsToString(suggestedCommandParts) + ChatColor.DARK_RED + "'!");
        }

        // Print the help header
        sender.sendMessage(ChatColor.GOLD + "==========[ " + Settings.helpHeader.toUpperCase() + " HELP ]==========");

        // Print the command help information
        if (showCommand)
            HelpPrinter.printCommand(sender, command, commandReference);
        if (showDescription)
            HelpPrinter.printCommandDescription(sender, command);
        if (showArguments)
            HelpPrinter.printArguments(sender, command);
        if (showPermissions)
            HelpPrinter.printPermissions(sender, command);
        if (showAlternatives)
            HelpPrinter.printAlternatives(sender, command, commandReference);
        if (showCommands)
            HelpPrinter.printChildren(sender, command, commandReference);
    }
}
