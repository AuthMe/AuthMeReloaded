package fr.xephi.authme.command;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.help.HelpProvider;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Service for implementations of {@link ExecutableCommand} to execute some common tasks.
 * This service basically wraps calls, forwarding them to other classes.
 */
public class CommandService {

    private final AuthMe authMe;
    private final Messages messages;
    private final HelpProvider helpProvider;
    private final CommandMapper commandMapper;

    /**
     * Constructor.
     *
     * @param authMe The plugin instance
     * @param commandMapper Command mapper
     * @param helpProvider Help provider
     * @param messages Messages instance
     */
    public CommandService(AuthMe authMe, CommandMapper commandMapper, HelpProvider helpProvider, Messages messages) {
        this.authMe = authMe;
        this.messages = messages;
        this.helpProvider = helpProvider;
        this.commandMapper = commandMapper;
    }

    /**
     * Send a message to a player.
     *
     * @param sender The command sender to send the message to
     * @param messageKey The message key to send
     */
    public void send(CommandSender sender, MessageKey messageKey) {
        messages.send(sender, messageKey);
    }

    /**
     * Map command parts to a command description.
     *
     * @param sender The command sender issuing the request (for permission check), or null to skip permissions
     * @param commandParts The received command parts to map to a command
     * @return The computed mapping result
     */
    public FoundCommandResult mapPartsToCommand(CommandSender sender, List<String> commandParts) {
        return commandMapper.mapPartsToCommand(sender, commandParts);
    }

    /**
     * Output the standard error message for the status in the provided {@link FoundCommandResult} object.
     * Does not output anything for successful mappings.
     *
     * @param sender The sender to output the error to
     * @param result The mapping result to process
     */
    public void outputMappingError(CommandSender sender, FoundCommandResult result) {
        commandMapper.outputStandardError(sender, result);
    }

    /**
     * Run the given task asynchronously with the Bukkit scheduler.
     *
     * @param task The task to run
     */
    public void runTaskAsynchronously(Runnable task) {
        authMe.getServer().getScheduler().runTaskAsynchronously(authMe, task);
    }

    /**
     * Return the AuthMe data source.
     *
     * @return The used data source
     */
    public DataSource getDataSource() {
        // TODO ljacqu 20151222: Add getter for .database and rename the field to dataSource
        return authMe.database;
    }

    /**
     * Output the help for a given command.
     *
     * @param sender The sender to output the help to
     * @param result The result to output information about
     * @param options Output options, see {@link HelpProvider}
     */
    public void outputHelp(CommandSender sender, FoundCommandResult result, int options) {
        List<String> lines = helpProvider.printHelp(sender, result, options);
        for (String line : lines) {
            sender.sendMessage(line);
        }
    }

}
