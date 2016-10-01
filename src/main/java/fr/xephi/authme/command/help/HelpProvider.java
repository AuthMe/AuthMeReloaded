package fr.xephi.authme.command.help;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import fr.xephi.authme.command.CommandArgumentDescription;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandUtils;
import fr.xephi.authme.command.FoundCommandResult;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.permission.DefaultPermission;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static fr.xephi.authme.command.help.HelpMessageKey.DETAILED_DESCRIPTION;
import static fr.xephi.authme.command.help.HelpMessageKey.SHORT_DESCRIPTION;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/**
 * Help syntax generator for AuthMe commands.
 */
public class HelpProvider implements SettingsDependent {

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

    private final PermissionsManager permissionsManager;
    private final HelpMessagesService helpMessagesService;
    private String helpHeader;

    @Inject
    HelpProvider(PermissionsManager permissionsManager, HelpMessagesService helpMessagesService, Settings settings) {
        this.permissionsManager = permissionsManager;
        this.helpMessagesService = helpMessagesService;
        reload(settings);
    }

    private List<String> printHelp(CommandSender sender, FoundCommandResult result, int options) {
        if (result.getCommandDescription() == null) {
            return singletonList(ChatColor.DARK_RED + "Failed to retrieve any help information!");
        }

        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GOLD + "==========[ " + helpHeader + " HELP ]==========");

        CommandDescription command = helpMessagesService.buildLocalizedDescription(result.getCommandDescription());
        List<String> labels = ImmutableList.copyOf(result.getLabels());
        List<String> correctLabels = ImmutableList.copyOf(filterCorrectLabels(command, labels));

        if (!hasFlag(HIDE_COMMAND, options)) {
            lines.add(ChatColor.GOLD + "Command: " + CommandSyntaxHelper.getSyntax(command, correctLabels));
        }
        if (hasFlag(SHOW_LONG_DESCRIPTION, options)) {
            printDetailedDescription(command, lines);
        }
        if (hasFlag(SHOW_ARGUMENTS, options)) {
            printArguments(command, lines);
        }
        if (hasFlag(SHOW_PERMISSIONS, options) && sender != null) {
            printPermissions(command, sender, lines);
        }
        if (hasFlag(SHOW_ALTERNATIVES, options)) {
            printAlternatives(command, correctLabels, lines);
        }
        if (hasFlag(SHOW_CHILDREN, options)) {
            printChildren(command, labels, lines);
        }

        return lines;
    }

    /**
     * Output the help for a given command.
     *
     * @param sender The sender to output the help to
     * @param result The result to output information about
     * @param options Output options, see {@link HelpProvider}
     */
    public void outputHelp(CommandSender sender, FoundCommandResult result, int options) {
        List<String> lines = printHelp(sender, result, options);
        for (String line : lines) {
            sender.sendMessage(line);
        }
    }

    @Override
    public void reload(Settings settings) {
        helpHeader = settings.getProperty(PluginSettings.HELP_HEADER);
    }

    private void printDetailedDescription(CommandDescription command, List<String> lines) {
        lines.add(ChatColor.GOLD + helpMessagesService.getMessage(SHORT_DESCRIPTION) + ": "
            + ChatColor.WHITE + command.getDescription());

        lines.add(ChatColor.GOLD + helpMessagesService.getMessage(DETAILED_DESCRIPTION) + ":");
        lines.add(ChatColor.WHITE + " " + command.getDetailedDescription());
    }

    private void printArguments(CommandDescription command, List<String> lines) {
        if (command.getArguments().isEmpty()) {
            return;
        }

        lines.add(ChatColor.GOLD + helpMessagesService.getMessage(HelpMessageKey.ARGUMENTS) + ":");
        StringBuilder argString = new StringBuilder();
        String optionalText = " (" + helpMessagesService.getMessage(HelpMessageKey.OPTIONAL) + ")";
        for (CommandArgumentDescription argument : command.getArguments()) {
            argString.setLength(0);
            argString.append(" ").append(ChatColor.YELLOW).append(ChatColor.ITALIC).append(argument.getName())
                .append(": ").append(ChatColor.WHITE).append(argument.getDescription());

            if (argument.isOptional()) {
                argString.append(ChatColor.GRAY).append(ChatColor.ITALIC).append(optionalText);
            }
            lines.add(argString.toString());
        }
    }

    private void printAlternatives(CommandDescription command, List<String> correctLabels, List<String> lines) {
        if (command.getLabels().size() <= 1 || correctLabels.size() <= 1) {
            return;
        }

        lines.add(ChatColor.GOLD + helpMessagesService.getMessage(HelpMessageKey.ALTERNATIVES) + ":");
        // Get the label used
        final String parentLabel = correctLabels.get(0);
        final String childLabel = correctLabels.get(1);

        // Create a list of alternatives
        for (String entry : command.getLabels()) {
            if (!entry.equalsIgnoreCase(childLabel)) {
                lines.add(" " + CommandSyntaxHelper.getSyntax(command, asList(parentLabel, entry)));
            }
        }
    }

    private void printPermissions(CommandDescription command, CommandSender sender, List<String> lines) {
        PermissionNode permission = command.getPermission();
        if (permission == null) {
            return;
        }
        lines.add(ChatColor.GOLD + helpMessagesService.getMessage(HelpMessageKey.PERMISSIONS) + ":");

        boolean hasPermission = permissionsManager.hasPermission(sender, permission);
        lines.add(String.format(" " + ChatColor.YELLOW + ChatColor.ITALIC + "%s" + ChatColor.GRAY + " (%s)",
            permission.getNode(), getLocalPermissionText(hasPermission)));

        // Addendum to the line to specify whether the sender has permission or not when default is OP_ONLY
        final DefaultPermission defaultPermission = permission.getDefaultPermission();
        String addendum = "";
        if (DefaultPermission.OP_ONLY.equals(defaultPermission)) {
            addendum = " (" + getLocalPermissionText(defaultPermission.evaluate(sender)) + ")";
        }
        lines.add(ChatColor.GOLD + helpMessagesService.getMessage(HelpMessageKey.DEFAULT) + ": "
            + ChatColor.GRAY + ChatColor.ITALIC + helpMessagesService.getMessage(defaultPermission) + addendum);

        // Evaluate if the sender has permission to the command
        ChatColor permissionColor;
        String permissionText;
        if (permissionsManager.hasPermission(sender, command.getPermission())) {
            permissionColor = ChatColor.GREEN;
            permissionText = getLocalPermissionText(true);
        } else {
            permissionColor = ChatColor.DARK_RED;
            permissionText = getLocalPermissionText(false);
        }
        lines.add(String.format(ChatColor.GOLD + " %s: %s" + ChatColor.ITALIC + "%s",
            helpMessagesService.getMessage(HelpMessageKey.RESULT), permissionColor, permissionText));
    }

    private String getLocalPermissionText(boolean hasPermission) {
        if (hasPermission) {
            return helpMessagesService.getMessage(HelpMessageKey.HAS_PERMISSION);
        }
        return helpMessagesService.getMessage(HelpMessageKey.NO_PERMISSION);
    }

    private void printChildren(CommandDescription command, List<String> parentLabels, List<String> lines) {
        if (command.getChildren().isEmpty()) {
            return;
        }

        lines.add(ChatColor.GOLD + helpMessagesService.getMessage(HelpMessageKey.COMMANDS) + ":");
        String parentCommandPath = CommandUtils.labelsToString(parentLabels);
        for (CommandDescription child : command.getChildren()) {
            lines.add(" /" + parentCommandPath + " " + child.getLabels().get(0)
                + ChatColor.GRAY + ChatColor.ITALIC + ": " + child.getDescription());
        }
    }

    private static boolean hasFlag(int flag, int options) {
        return (flag & options) != 0;
    }

    @VisibleForTesting
    protected static List<String> filterCorrectLabels(CommandDescription command, List<String> labels) {
        List<CommandDescription> commands = CommandUtils.constructParentList(command);
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
