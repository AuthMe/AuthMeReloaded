package fr.xephi.authme.command.help;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandArgumentDescription;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.CommandPermissions;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.util.CollectionUtils;
import fr.xephi.authme.util.StringUtils;

/**
 */
public class HelpPrinter {

    /**
     * Print the command help information.
     *
     * @param sender           The command sender to print the help to.
     * @param command          The command to print.
     * @param commandReference The command reference used.
     */
    public static void printCommand(CommandSender sender, CommandDescription command, CommandParts commandReference) {
        // Print the proper command syntax
        sender.sendMessage(ChatColor.GOLD + "Command: " + HelpSyntaxHelper.getCommandSyntax(command, commandReference, null, true));
    }

    /**
     * Print the command help description information. This will print both the short, as the detailed description if available.
     *
     * @param sender  The command sender to print the help to.
     * @param command The command to print the description help for.
     */
    public static void printCommandDescription(CommandSender sender, CommandDescription command) {
        sender.sendMessage(ChatColor.GOLD + "Short Description: " + ChatColor.WHITE + command.getDescription());

        // Print the detailed description, if available
        if (!StringUtils.isEmpty(command.getDetailedDescription())) {
            sender.sendMessage(ChatColor.GOLD + "Detailed Description:");
            sender.sendMessage(ChatColor.WHITE + " " + command.getDetailedDescription());
        }
    }

    /**
     * Print the command help arguments information if available.
     *
     * @param sender  The command sender to print the help to.
     * @param command The command to print the argument help for.
     */
    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    public static void printArguments(CommandSender sender, CommandDescription command) {
        // Make sure there are any commands to print
        if (!command.hasArguments())
            return;

        // Print the header
        sender.sendMessage(ChatColor.GOLD + "Arguments:");

        // Print each argument
        for (CommandArgumentDescription arg : command.getArguments()) {
            // Create a string builder to build the syntax in
            StringBuilder argString = new StringBuilder();
            argString.append(" " + ChatColor.YELLOW + ChatColor.ITALIC + arg.getLabel() + " : " + ChatColor.WHITE + arg.getDescription());

            // Suffix a note if the command is optional
            if (arg.isOptional())
                argString.append(ChatColor.GRAY + "" + ChatColor.ITALIC + " (Optional)");

            // Print the syntax
            sender.sendMessage(argString.toString());
        }
    }

    /**
     * Print the command help permissions information if available.
     *
     * @param sender  The command sender to print the help to.
     * @param command The command to print the permissions help for.
     */
    public static void printPermissions(CommandSender sender, CommandDescription command) {
        // Get the permissions and make sure they aren't missing
        CommandPermissions permissions = command.getCommandPermissions();
        if (permissions == null || CollectionUtils.isEmpty(permissions.getPermissionNodes())) {
            return;
        }

        // Print the header
        sender.sendMessage(ChatColor.GOLD + "Permissions:");

        // Print each node
        for (PermissionNode node : permissions.getPermissionNodes()) {
            boolean nodePermission = true;
            if (sender instanceof Player)
                nodePermission = AuthMe.getInstance().getPermissionsManager().hasPermission((Player) sender, node);
            final String nodePermsString = ChatColor.GRAY + (nodePermission ? ChatColor.ITALIC + " (Permission!)" : ChatColor.ITALIC + " (No Permission!)");
            sender.sendMessage(" " + ChatColor.YELLOW + ChatColor.ITALIC + node + nodePermsString);
        }

        // Print the default permission
        // TODO ljacqu 20151205: This is duplicating the logic in PermissionsManager#evaluateDefaultPermission
        // Either use the command manager here, or if that's too heavy, look into moving certain permissions logic
        // into a Utils class
        switch (permissions.getDefaultPermission()) {
            case ALLOWED:
                sender.sendMessage(ChatColor.GOLD + " Default: " + ChatColor.GRAY + ChatColor.ITALIC + "Permission!");
                break;

            case OP_ONLY:
                final String defaultPermsString = ChatColor.GRAY + (sender.isOp() ? ChatColor.ITALIC + " (Permission!)" : ChatColor.ITALIC + " (No Permission!)");
                sender.sendMessage(ChatColor.GOLD + " Default: " + ChatColor.YELLOW + ChatColor.ITALIC + "OP's Only!" + defaultPermsString);
                break;

            case NOT_ALLOWED:
            default:
                sender.sendMessage(ChatColor.GOLD + " Default: " + ChatColor.GRAY + ChatColor.ITALIC + "No Permission!");
                break;
        }

        // Print the permission result
        if (permissions.hasPermission(sender))
            sender.sendMessage(ChatColor.GOLD + " Result: " + ChatColor.GREEN + ChatColor.ITALIC + "Permission!");
        else
            sender.sendMessage(ChatColor.GOLD + " Result: " + ChatColor.DARK_RED + ChatColor.ITALIC + "No Permission!");
    }

    /**
     * Print the command help alternatives information if available.
     *
     * @param sender           The command sender to print the help to.
     * @param command          The command used.
     * @param commandReference The original command reference used for this command.
     */
    public static void printAlternatives(CommandSender sender, CommandDescription command, CommandParts commandReference) {
        // Make sure there are any alternatives
        if (command.getLabels().size() <= 1)
            return;

        // Print the header
        sender.sendMessage(ChatColor.GOLD + "Alternatives:");

        // Get the label used
        final String usedLabel = commandReference.get(command.getParentCount());

        // Create a list of alternatives
        List<String> alternatives = new ArrayList<>();
        for (String entry : command.getLabels()) {
            // Exclude the proper argument
            if (entry.equalsIgnoreCase(usedLabel))
                continue;
            alternatives.add(entry);
        }

        // Sort the alternatives
        Collections.sort(alternatives, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return Double.compare(StringUtils.getDifference(usedLabel, o1), StringUtils.getDifference(usedLabel, o2));
            }
        });

        // Print each alternative with proper syntax
        for (String alternative : alternatives)
            sender.sendMessage(" " + HelpSyntaxHelper.getCommandSyntax(command, commandReference, alternative, true));
    }

    /**
     * Print the command help child's information if available.
     *
     * @param sender           The command sender to print the help to.
     * @param command          The command to print the help for.
     * @param commandReference The original command reference used for this command.
     */
    public static void printChildren(CommandSender sender, CommandDescription command, CommandParts commandReference) {
        // Make sure there are child's
        if (command.getChildren().size() <= 0)
            return;

        // Print the header
        sender.sendMessage(ChatColor.GOLD + "Commands:");

        // Loop through each child
        for (CommandDescription child : command.getChildren())
            sender.sendMessage(" " + HelpSyntaxHelper.getCommandSyntax(child, commandReference, null, false) + ChatColor.GRAY + ChatColor.ITALIC + " : " + child.getDescription());
    }
}
