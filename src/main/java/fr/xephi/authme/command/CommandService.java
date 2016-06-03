package fr.xephi.authme.command;

import fr.xephi.authme.command.help.HelpProvider;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.util.ValidationService;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;

/**
 * Service for implementations of {@link ExecutableCommand} to execute some common tasks.
 * This service basically wraps calls, forwarding them to other classes.
 */
public class CommandService {

    @Inject
    private Messages messages;
    @Inject
    private HelpProvider helpProvider;
    @Inject
    private CommandMapper commandMapper;
    @Inject
    private NewSetting settings;
    @Inject
    private ValidationService validationService;

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
     * Send a message to a player.
     *
     * @param sender The command sender to send the message to
     * @param messageKey The message key to send
     * @param replacements The replacement arguments for the message key's tags
     */
    public void send(CommandSender sender, MessageKey messageKey, String... replacements) {
        messages.send(sender, messageKey, replacements);
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

    /**
     * Retrieve a message by its message key.
     *
     * @param key The message to retrieve
     * @return The message
     */
    public String[] retrieveMessage(MessageKey key) {
        return messages.retrieve(key);
    }

    /**
     * Retrieve the given property's value.
     *
     * @param property The property to retrieve
     * @param <T> The type of the property
     * @return The property's value
     */
    public <T> T getProperty(Property<T> property) {
        return settings.getProperty(property);
    }

    /**
     * Return the settings manager.
     *
     * @return The settings manager
     */
    public NewSetting getSettings() {
        return settings;
    }

    public boolean validateEmail(String email) {
        return validationService.validateEmail(email);
    }

    public boolean isEmailFreeForRegistration(String email, CommandSender sender) {
        return validationService.isEmailFreeForRegistration(email, sender);
    }

}
