package fr.xephi.authme.command;

import ch.jalu.injector.factory.Factory;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.help.HelpProvider;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The AuthMe command handler, responsible for invoking the correct {@link ExecutableCommand} based on incoming
 * command labels or for displaying a help message for unknown command labels.
 */
public class CommandHandler {

    /**
     * The threshold for suggesting a similar command. If the difference is below this value, we will
     * ask the player whether he meant the similar command.
     */
    private static final double SUGGEST_COMMAND_THRESHOLD = 0.75;

    private final CommandMapper commandMapper;
    private final PermissionsManager permissionsManager;
    private final Messages messages;
    private final HelpProvider helpProvider;

    /**
     * Map with ExecutableCommand children. The key is the type of the value.
     */
    private Map<Class<? extends ExecutableCommand>, ExecutableCommand> commands = new HashMap<>();

    @Inject
    CommandHandler(Factory<ExecutableCommand> commandFactory, CommandMapper commandMapper,
                   PermissionsManager permissionsManager, Messages messages, HelpProvider helpProvider) {
        this.commandMapper = commandMapper;
        this.permissionsManager = permissionsManager;
        this.messages = messages;
        this.helpProvider = helpProvider;
        initializeCommands(commandFactory, commandMapper.getCommandClasses());
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

        FoundCommandResult result = commandMapper.mapPartsToCommand(sender, parts);
        handleCommandResult(sender, result);
        return !FoundResultStatus.MISSING_BASE_COMMAND.equals(result.getResultStatus());
    }

    /**
     * Processes the given {@link FoundCommandResult} for the provided command sender.
     *
     * @param sender the command sender who executed the command
     * @param result the command mapping result
     */
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
                messages.send(sender, MessageKey.NO_PERMISSION);
                break;
            default:
                throw new IllegalStateException("Unknown result status '" + result.getResultStatus() + "'");
        }
    }

    /**
     * Initialize all required ExecutableCommand objects.
     *
     * @param commandFactory factory to create command objects
     * @param commandClasses the classes to instantiate
     */
    private void initializeCommands(Factory<ExecutableCommand> commandFactory,
                                    Set<Class<? extends ExecutableCommand>> commandClasses) {
        for (Class<? extends ExecutableCommand> clazz : commandClasses) {
            commands.put(clazz, commandFactory.newInstance(clazz));
        }
    }

    /**
     * Execute the command for the given command sender.
     *
     * @param sender The sender which initiated the command
     * @param result The mapped result
     */
    private void executeCommand(CommandSender sender, FoundCommandResult result) {
        ExecutableCommand executableCommand = commands.get(result.getCommandDescription().getExecutableCommand());
        List<String> arguments = result.getArguments();
        executableCommand.executeCommand(sender, arguments);
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
            if (!StringUtils.isBlank(argument)) {
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
        if (!permissionsManager.hasPermission(sender, command.getPermission())) {
            messages.send(sender, MessageKey.NO_PERMISSION);
            return;
        }

        ExecutableCommand executableCommand = commands.get(command.getExecutableCommand());
        MessageKey usageMessage = executableCommand.getArgumentsMismatchMessage();
        if (usageMessage == null) {
            showHelpForCommand(sender, result);
        } else {
            messages.send(sender, usageMessage);
        }
    }

    private void showHelpForCommand(CommandSender sender, FoundCommandResult result) {
        sender.sendMessage(ChatColor.DARK_RED + "Incorrect command arguments!");
        helpProvider.outputHelp(sender, result, HelpProvider.SHOW_ARGUMENTS);

        List<String> labels = result.getLabels();
        String childLabel = labels.size() >= 2 ? labels.get(1) : "";
        sender.sendMessage(ChatColor.GOLD + "Detailed help: " + ChatColor.WHITE
            + "/" + labels.get(0) + " help " + childLabel);
    }
}
