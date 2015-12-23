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

    public CommandService(AuthMe authMe, CommandMapper commandMapper, HelpProvider helpProvider, Messages messages) {
        this.authMe = authMe;
        this.messages = messages;
        this.helpProvider = helpProvider;
        this.commandMapper = commandMapper;
    }

    public void send(CommandSender sender, MessageKey messageKey) {
        messages.send(sender, messageKey);
    }

    public FoundCommandResult mapPartsToCommand(CommandSender sender, List<String> commandParts) {
        return commandMapper.mapPartsToCommand(sender, commandParts);
    }

    public void outputMappingError(CommandSender sender, FoundCommandResult result) {
        commandMapper.outputStandardError(sender, result);
    }

    public void runTaskAsynchronously(Runnable task) {
        authMe.getServer().getScheduler().runTaskAsynchronously(authMe, task);
    }

    public DataSource getDataSource() {
        // TODO ljacqu 20151222: Add getter for .database and rename the field to dataSource
        return authMe.database;
    }

    public void outputHelp(CommandSender sender, FoundCommandResult result, int options) {
        List<String> lines = helpProvider.printHelp(sender, result, options);
        for (String line : lines) {
            sender.sendMessage(line);
        }
    }

}
