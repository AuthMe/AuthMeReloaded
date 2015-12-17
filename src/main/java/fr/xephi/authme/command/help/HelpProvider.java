package fr.xephi.authme.command.help;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import fr.xephi.authme.command.CommandArgumentDescription;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandPermissions;
import fr.xephi.authme.command.CommandUtils;
import fr.xephi.authme.command.FoundCommandResult;
import fr.xephi.authme.permission.DefaultPermission;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.CollectionUtils;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.util.Collections.singletonList;

/**
 * Help syntax generator for AuthMe commands.
 */
public final class HelpProvider {

    // --- Bit flags ---
    /** Set to <i>not</i> show the command. */
    public static final int HIDE_COMMAND          = 0x001;
    /** Set to show the detailed description of a command. */
    public static final int SHOW_LONG_DESCRIPTION = 0x002;
    /** Set to include the arguments the command takes. */
    public static final int SHOW_ARGUMENTS        = 0x004;
    /** Set to show the permissions required to execute the command. */
    public static final int SHOW_PERMISSIONS      = 0x008;
    /** Set to show alternative labels for the command. */
    public static final int SHOW_ALTERNATIVES     = 0x010;
    /** Set to show the child commands of the command. */
    public static final int SHOW_CHILDREN         = 0x020;

    /** Shortcut for setting all options apart from {@link HelpProvider#HIDE_COMMAND}. */
    public static final int ALL_OPTIONS = ~HIDE_COMMAND;

    private HelpProvider() {
    }

    public static List<String> printHelp(FoundCommandResult foundCommand, int options) {
        return printHelp(foundCommand, null, null, options);
    }

    // sender and permissions manager may be null if SHOW_PERMISSIONS is not set
    public static List<String> printHelp(FoundCommandResult foundCommand, CommandSender sender,
                                         PermissionsManager permissionsManager, int options) {
        if (foundCommand.getCommandDescription() == null) {
            return singletonList(ChatColor.DARK_RED + "Failed to retrieve any help information!");
        }

        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GOLD + "==========[ " + Settings.helpHeader + " HELP ]==========");

        CommandDescription command = foundCommand.getCommandDescription();
        List<String> labels = ImmutableList.copyOf(foundCommand.getLabels());
        List<String> correctLabels = ImmutableList.copyOf(filterCorrectLabels(command, labels));

        if (!hasFlag(HIDE_COMMAND, options)) {
            printCommand(command, correctLabels, lines);
        }
        if (hasFlag(SHOW_LONG_DESCRIPTION, options)) {
            printDetailedDescription(command, lines);
        }
        if (hasFlag(SHOW_ARGUMENTS, options)) {
            printArguments(command, lines);
        }
        if (hasFlag(SHOW_PERMISSIONS, options) && sender != null && permissionsManager != null) {
            printPermissions(command, sender, permissionsManager, lines);
        }
        if (hasFlag(SHOW_ALTERNATIVES, options)) {
            printAlternatives(command, correctLabels, lines);
        }
        if (hasFlag(SHOW_CHILDREN, options)) {
            printChildren(command, labels, lines);
        }

        return lines;
    }

    private static void printCommand(CommandDescription command, List<String> correctLabels, List<String> lines) {
        // FIXME: Create highlight logic to mark arguments and the 2nd label as yellow
        String syntaxLine = "/" + CommandUtils.labelsToString(correctLabels);
        for (CommandArgumentDescription argument : command.getArguments()) {
            syntaxLine += " " + formatArgument(argument);
        }
        lines.add(syntaxLine);
    }

    private static void printDetailedDescription(CommandDescription command, List<String> lines) {
        lines.add(ChatColor.GOLD + "Short Description: " + ChatColor.WHITE + command.getDescription());
        lines.add(ChatColor.GOLD + "Detailed Description:");
        lines.add(ChatColor.WHITE + " " + command.getDetailedDescription());
    }

    private static void printArguments(CommandDescription command, List<String> lines) {
        if (!command.getArguments().isEmpty()) {
            return;
        }

        lines.add(ChatColor.GOLD + "Arguments:");
        for (CommandArgumentDescription argument : command.getArguments()) {
            StringBuilder argString = new StringBuilder();
            argString.append(" ").append(ChatColor.YELLOW).append(ChatColor.ITALIC).append(argument.getName())
                .append(": ").append(ChatColor.WHITE).append(argument.getDescription());

            if (argument.isOptional()) {
                argString.append(ChatColor.GRAY).append(ChatColor.ITALIC).append(" (Optional)");
            }
            lines.add(argString.toString());
        }
    }

    private static void printAlternatives(CommandDescription command, List<String> correctLabels, List<String> lines) {
        if (command.getLabels().size() <= 1) {
            return;
        }

        lines.add(ChatColor.GOLD + "Alternatives:");
        // Get the label used
        final String usedLabel = correctLabels.get(correctLabels.size() - 1);

        // Create a list of alternatives
        List<String> alternatives = new ArrayList<>();
        for (String entry : command.getLabels()) {
            if (!entry.equalsIgnoreCase(usedLabel)) {
                alternatives.add(entry);
            }
        }

        // Sort the alternatives
        Collections.sort(alternatives, new Comparator<String>() {
            // TODO ljacqu 20151212: This computes the difference each time anew. It might make sense to compute the
            // difference once and to store it in some map-like structure (Guava has some interesting ones)
            @Override
            public int compare(String o1, String o2) {
                return Double.compare(StringUtils.getDifference(usedLabel, o1),
                    StringUtils.getDifference(usedLabel, o2));
            }
        });

        // Print each alternative with proper syntax
        for (String alternative : alternatives) {
            // fixme add highlight functionality (see commented old line)
            // sender.sendMessage(" " + _HelpSyntaxHelper.getCommandSyntax(command, commandReference, alternative, true));
            lines.add(" " + CommandUtils.labelsToString(correctLabels) + " " + alternative);
        }
    }

    private static void printPermissions(CommandDescription command, CommandSender sender,
                                        PermissionsManager permissionsManager, List<String> lines) {
        CommandPermissions permissions = command.getCommandPermissions();
        if (permissions == null || CollectionUtils.isEmpty(permissions.getPermissionNodes())) {
            return;
        }
        lines.add(ChatColor.GOLD + "Permissions:");

        for (PermissionNode node : permissions.getPermissionNodes()) {
            boolean hasPermission = permissionsManager.hasPermission(sender, node);
            final String nodePermsString = "" + ChatColor.GRAY + ChatColor.ITALIC
                + (hasPermission ? " (You have permission)" : " (No permission)");
            lines.add(" " + ChatColor.YELLOW + ChatColor.ITALIC + node.getNode() + nodePermsString);
        }

        // Addendum to the line to specify whether the sender has permission or not when default is OP_ONLY
        final DefaultPermission defaultPermission = permissions.getDefaultPermission();
        String addendum = "";
        if (DefaultPermission.OP_ONLY.equals(defaultPermission)) {
            addendum = PermissionsManager.evaluateDefaultPermission(defaultPermission, sender)
                ? " (You have permission)"
                : " (No permission)";
        }
        lines.add(ChatColor.GOLD + "Default: " + ChatColor.GRAY + ChatColor.ITALIC
            + defaultPermission.getTitle() + addendum);

        // Evaluate if the sender has permission to the command
        if (permissionsManager.hasPermission(sender, command)) {
            lines.add(ChatColor.GOLD + " Result: " + ChatColor.GREEN + ChatColor.ITALIC + "You have permission");
        } else {
            lines.add(ChatColor.GOLD + " Result: " + ChatColor.DARK_RED + ChatColor.ITALIC + "No permission");
        }
    }

    private static void printChildren(CommandDescription command, List<String> parentLabels, List<String> lines) {
        if (command.getChildren().isEmpty()) {
            return;
        }

        lines.add(ChatColor.GOLD + "Commands:");
        String parentCommandPath = CommandUtils.labelsToString(parentLabels);
        for (CommandDescription child : command.getChildren()) {
            lines.add(" " + parentCommandPath + child.getLabels().get(0)
                + ChatColor.GRAY + ChatColor.ITALIC + ": " + child.getDescription());
        }
    }

    /** Format a command argument with the proper type of brackets. */
    private static String formatArgument(CommandArgumentDescription argument) {
        if (argument.isOptional()) {
            return " [" + argument.getName() + "]";
        }
        return " <" + argument.getName() + ">";
    }

    private static boolean hasFlag(int flag, int options) {
        return (flag & options) != 0;
    }

    private static List<String> filterCorrectLabels(CommandDescription command, List<String> labels) {
        List<CommandDescription> commands = new ArrayList<>(command.getParentCount() + 1);
        CommandDescription currentCommand = command;
        while (currentCommand != null) {
            commands.add(command);
            currentCommand = currentCommand.getParent();
        }
        commands = Lists.reverse(commands);

        List<String> correctLabels = new ArrayList<>();
        boolean foundIncorrectLabel = false;
        for (int i = 0; i < commands.size(); ++i) {
            if (!foundIncorrectLabel && i < labels.size() && commands.get(i).hasLabel(labels.get(i))) {
                correctLabels.add(labels.get(i));
            } else {
                foundIncorrectLabel = true;
                correctLabels.add(commands.get(i).getLabels().get(0));
            }
        }
        return correctLabels;
    }

}
