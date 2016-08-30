package fr.xephi.authme.command;

import com.github.authme.configme.properties.Property;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.ValidationService;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;

/**
 * Service for implementations of {@link ExecutableCommand} to execute some common tasks.
 * This service basically wraps calls, forwarding them to other classes.
 */
public class CommandService {

    @Inject
    private Messages messages;
    @Inject
    private Settings settings;
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
     * Retrieve a message by its message key.
     *
     * @param key The message to retrieve
     * @return The message
     */
    public String[] retrieveMessage(MessageKey key) {
        return messages.retrieve(key);
    }

    /**
     * Retrieve a message as a single String by its message key.
     *
     * @param key The message to retrieve
     * @return The message
     */
    public String retrieveSingle(MessageKey key) {
        return messages.retrieveSingle(key);
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
    public Settings getSettings() {
        return settings;
    }

    public boolean validateEmail(String email) {
        return validationService.validateEmail(email);
    }

    public boolean isEmailFreeForRegistration(String email, CommandSender sender) {
        return validationService.isEmailFreeForRegistration(email, sender);
    }

}
