package fr.xephi.authme.command;

import java.util.ArrayList;
import java.util.List;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.help.HelpProvider;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import fr.xephi.authme.util.StringUtils;

/**
 * The AuthMe command handler, responsible for mapping incoming commands to the correct {@link CommandDescription}
 * or to display help messages for unknown invocations.
 */
public class CommandHandler {

    /**
     * The threshold for suggesting a similar command. If the difference is below this value, we will
     * ask the player whether he meant the similar command.
     */
    private static final double SUGGEST_COMMAND_THRESHOLD = 0.75;

    private final CommandService commandService;

    /**
     * Create a command handler.
     * 
     * @param commandService The CommandService instance
     */
    public CommandHandler(CommandService commandService) {
        this.commandService = commandService;
    }

    /**
     * Map a command that was invoked to the proper {@link CommandDescription} or return a useful error
     * message upon failure.
     *
     * @param sender             The command sender.
     * @param bukkitCommandLabel The command label (Bukkit).
     * @param bukkitArgs         The command arguments (Bukkit).
     *
     * @return True if the command was executed, false otherwise.
     */
    public boolean processCommand(CommandSender sender, String bukkitCommandLabel, String[] bukkitArgs) {
        // Add the Bukkit command label to the front so we get a list like [authme, register, bobby, mysecret]
        List<String> parts = skipEmptyArguments(bukkitArgs);
        parts.add(0, bukkitCommandLabel);

        FoundCommandResult result = commandService.mapPartsToCommand(sender, parts);
        handleCommandResult(sender, result);
        return !FoundResultStatus.MISSING_BASE_COMMAND.equals(result.getResultStatus());
    }

    private void handleCommandResult(CommandSender sender, FoundCommandResult result) {
        switch (result.getResultStatus()) {
            case SUCCESS:
                executeCommand(sender, result);
                break;
            case MISSING_BASE_COMMAND:
                sender.sendMessage(ChatColor.DARK_RED + "Failed to parse " + AuthMe.getPluginName() + " command!");
                break;
            case INCORRECT_ARGUMENTS:
                sendImproperArgumentsMessage(sender, result);
                break;
            case UNKNOWN_LABEL:
                sendUnknownCommandMessage(sender, result);
                break;
            case NO_PERMISSION:
                sendPermissionDeniedError(sender);
                break;
            default:
                throw new IllegalStateException("Unknown result status '" + result.getResultStatus() + "'");
        }
    }

    /**
     * Execute the command for the given command sender.
     *
     * @param sender The sender which initiated the command
     * @param result The mapped result
     */
    private void executeCommand(CommandSender sender, FoundCommandResult result) {
        ExecutableCommand executableCommand = result.getCommandDescription().getExecutableCommand();
        List<String> arguments = result.getArguments();
        executableCommand.executeCommand(sender, arguments, commandService);
    }

    /**
     * Skip all entries of the given array that are simply whitespace.
     *
     * @param args The array to process
     * @return List of the items that are not empty
     */
    private static List<String> skipEmptyArguments(String[] args) {
        List<String> cleanArguments = new ArrayList<>();
        for (String argument : args) {
            if (!StringUtils.isEmpty(argument)) {
                cleanArguments.add(argument);
            }
        }
        return cleanArguments;
    }

    /**
     * Show an "unknown command" message to the user and suggest an existing command if its similarity is within
     * the defined threshold.
     *
     * @param sender The command sender
     * @param result The command that was found during the mapping process
     */
    private static void sendUnknownCommandMessage(CommandSender sender, FoundCommandResult result) {
        sender.sendMessage(ChatColor.DARK_RED + "Unknown command!");

        // Show a command suggestion if available and the difference isn't too big
        if (result.getDifference() <= SUGGEST_COMMAND_THRESHOLD && result.getCommandDescription() != null) {
            sender.sendMessage(ChatColor.YELLOW + "Did you mean " + ChatColor.GOLD
                + CommandUtils.constructCommandPath(result.getCommandDescription()) + ChatColor.YELLOW + "?");
        }

        sender.sendMessage(ChatColor.YELLOW + "Use the command " + ChatColor.GOLD + "/" + result.getLabels().get(0)
            + " help" + ChatColor.YELLOW + " to view help.");
    }

    private void sendImproperArgumentsMessage(CommandSender sender, FoundCommandResult result) {
        CommandDescription command = result.getCommandDescription();
        if (!commandService.getPermissionsManager().hasPermission(sender, command)) {
            sendPermissionDeniedError(sender);
            return;
        }

        // Show the command argument help
        sender.sendMessage(ChatColor.DARK_RED + "Incorrect command arguments!");
        commandService.outputHelp(sender, result, HelpProvider.SHOW_ARGUMENTS);

        List<String> labels = result.getLabels();
        String childLabel = labels.size() >= 2 ? labels.get(1) : "";
        sender.sendMessage(ChatColor.GOLD + "Detailed help: " + ChatColor.WHITE
            + "/" + labels.get(0) + " help " + childLabel);
    }

    // TODO ljacqu 20151212: Remove me once I am a MessageKey
    private static void sendPermissionDeniedError(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to use this command!");
    }

}
