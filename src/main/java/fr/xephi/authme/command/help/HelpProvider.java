package fr.xephi.authme.command.help;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import fr.xephi.authme.command.CommandArgumentDescription;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandUtils;
import fr.xephi.authme.command.FoundCommandResult;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.permission.DefaultPermission;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static fr.xephi.authme.command.help.HelpSection.DETAILED_DESCRIPTION;
import static fr.xephi.authme.command.help.HelpSection.SHORT_DESCRIPTION;
import static java.util.Collections.singletonList;

/**
 * Help syntax generator for AuthMe commands.
 */
public class HelpProvider implements Reloadable {

    // --- Bit flags ---
    /** Set to show a command overview. */
    public static final int SHOW_COMMAND          = 0x001;
    /** Set to show the description of the command. */
    public static final int SHOW_DESCRIPTION      = 0x002;
    /** Set to show the detailed description of the command. */
    public static final int SHOW_LONG_DESCRIPTION = 0x004;
    /** Set to include the arguments the command takes. */
    public static final int SHOW_ARGUMENTS        = 0x008;
    /** Set to show the permissions required to execute the command. */
    public static final int SHOW_PERMISSIONS      = 0x010;
    /** Set to show alternative labels for the command. */
    public static final int SHOW_ALTERNATIVES     = 0x020;
    /** Set to show the child commands of the command. */
    public static final int SHOW_CHILDREN         = 0x040;

    /** Shortcut for setting all options. */
    public static final int ALL_OPTIONS = ~0;

    private final PermissionsManager permissionsManager;
    private final HelpMessagesService helpMessagesService;
    /** int with bit flags set corresponding to the above constants for enabled sections. */
    private Integer enabledSections;

    @Inject
    HelpProvider(PermissionsManager permissionsManager, HelpMessagesService helpMessagesService) {
        this.permissionsManager = permissionsManager;
        this.helpMessagesService = helpMessagesService;
    }

    private List<String> printHelp(CommandSender sender, FoundCommandResult result, int options) {
        if (result.getCommandDescription() == null) {
            return singletonList(ChatColor.DARK_RED + "Failed to retrieve any help information!");
        }

        List<String> lines = new ArrayList<>();
        options = filterDisabledSections(options);
        if (options == 0) {
            // Return directly if no options are enabled so we don't include the help header
            return lines;
        }
        String header = helpMessagesService.getMessage(HelpMessage.HEADER);
        if (!header.isEmpty()) {
            lines.add(ChatColor.GOLD + header);
        }

        CommandDescription command = helpMessagesService.buildLocalizedDescription(result.getCommandDescription());
        List<String> labels = ImmutableList.copyOf(result.getLabels());
        List<String> correctLabels = ImmutableList.copyOf(filterCorrectLabels(command, labels));

        if (hasFlag(SHOW_COMMAND, options)) {
            lines.add(ChatColor.GOLD + helpMessagesService.getMessage(HelpSection.COMMAND) + ": "
                + CommandUtils.buildSyntax(command, correctLabels));
        }
        if (hasFlag(SHOW_DESCRIPTION, options)) {
            lines.add(ChatColor.GOLD + helpMessagesService.getMessage(SHORT_DESCRIPTION) + ": "
                + ChatColor.WHITE + command.getDescription());
        }
        if (hasFlag(SHOW_LONG_DESCRIPTION, options)) {
            lines.add(ChatColor.GOLD + helpMessagesService.getMessage(DETAILED_DESCRIPTION) + ":");
            lines.add(ChatColor.WHITE + " " + command.getDetailedDescription());
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
    public void reload() {
        // We don't know about the reloading order of the classes, i.e. we cannot assume that HelpMessagesService
        // has already been reloaded. So set the enabledSections flag to null and redefine it first time needed.
        enabledSections = null;
    }

    /**
     * Removes any disabled sections from the options. Sections are considered disabled
     * if the translated text for the section is empty.
     *
     * @param options the options to process
     * @return the options without any disabled sections
     */
    private int filterDisabledSections(int options) {
        if (enabledSections == null) {
            enabledSections = flagFor(HelpSection.COMMAND, SHOW_COMMAND)
                | flagFor(HelpSection.SHORT_DESCRIPTION, SHOW_DESCRIPTION)
                | flagFor(HelpSection.DETAILED_DESCRIPTION, SHOW_LONG_DESCRIPTION)
                | flagFor(HelpSection.ARGUMENTS, SHOW_ARGUMENTS)
                | flagFor(HelpSection.PERMISSIONS, SHOW_PERMISSIONS)
                | flagFor(HelpSection.ALTERNATIVES, SHOW_ALTERNATIVES)
                | flagFor(HelpSection.CHILDREN, SHOW_CHILDREN);
        }
        return options & enabledSections;
    }

    private int flagFor(HelpSection section, int flag) {
        return helpMessagesService.getMessage(section).isEmpty() ? 0 : flag;
    }

    private void printArguments(CommandDescription command, List<String> lines) {
        if (command.getArguments().isEmpty()) {
            return;
        }

        lines.add(ChatColor.GOLD + helpMessagesService.getMessage(HelpSection.ARGUMENTS) + ":");
        StringBuilder argString = new StringBuilder();
        String optionalText = " (" + helpMessagesService.getMessage(HelpMessage.OPTIONAL) + ")";
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
        if (command.getLabels().size() <= 1) {
            return;
        }

        lines.add(ChatColor.GOLD + helpMessagesService.getMessage(HelpSection.ALTERNATIVES) + ":");

        // Label with which the command was called -> don't show it as an alternative
        final String usedLabel;
        // Takes alternative label and constructs list of labels, e.g. "reg" -> [authme, reg]
        final Function<String, List<String>> commandLabelsFn;

        if (correctLabels.size() == 1) {
            usedLabel = correctLabels.get(0);
            commandLabelsFn = label -> singletonList(label);
        } else {
            usedLabel = correctLabels.get(1);
            commandLabelsFn = label -> Arrays.asList(correctLabels.get(0), label);
        }

        // Create a list of alternatives
        for (String label : command.getLabels()) {
            if (!label.equalsIgnoreCase(usedLabel)) {
                lines.add(" " + CommandUtils.buildSyntax(command, commandLabelsFn.apply(label)));
            }
        }
    }

    private void printPermissions(CommandDescription command, CommandSender sender, List<String> lines) {
        PermissionNode permission = command.getPermission();
        if (permission == null) {
            return;
        }
        lines.add(ChatColor.GOLD + helpMessagesService.getMessage(HelpSection.PERMISSIONS) + ":");

        boolean hasPermission = permissionsManager.hasPermission(sender, permission);
        lines.add(String.format(" " + ChatColor.YELLOW + ChatColor.ITALIC + "%s" + ChatColor.GRAY + " (%s)",
            permission.getNode(), getLocalPermissionText(hasPermission)));

        // Addendum to the line to specify whether the sender has permission or not when default is OP_ONLY
        final DefaultPermission defaultPermission = permission.getDefaultPermission();
        String addendum = "";
        if (DefaultPermission.OP_ONLY.equals(defaultPermission)) {
            addendum = " (" + getLocalPermissionText(defaultPermission.evaluate(sender)) + ")";
        }
        lines.add(ChatColor.GOLD + helpMessagesService.getMessage(HelpMessage.DEFAULT) + ": "
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
            helpMessagesService.getMessage(HelpMessage.RESULT), permissionColor, permissionText));
    }

    private String getLocalPermissionText(boolean hasPermission) {
        if (hasPermission) {
            return helpMessagesService.getMessage(HelpMessage.HAS_PERMISSION);
        }
        return helpMessagesService.getMessage(HelpMessage.NO_PERMISSION);
    }

    private void printChildren(CommandDescription command, List<String> parentLabels, List<String> lines) {
        if (command.getChildren().isEmpty()) {
            return;
        }

        lines.add(ChatColor.GOLD + helpMessagesService.getMessage(HelpSection.CHILDREN) + ":");
        String parentCommandPath = String.join(" ", parentLabels);
        for (CommandDescription child : command.getChildren()) {
            lines.add(" /" + parentCommandPath + " " + child.getLabels().get(0)
                + ChatColor.GRAY + ChatColor.ITALIC + ": " + helpMessagesService.getDescription(child));
        }
    }

    private static boolean hasFlag(int flag, int options) {
        return (flag & options) != 0;
    }

    @VisibleForTesting
    static List<String> filterCorrectLabels(CommandDescription command, List<String> labels) {
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
