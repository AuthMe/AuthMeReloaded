package fr.xephi.authme.process;

import com.github.authme.configme.properties.Property;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.permission.AuthGroupHandler;
import fr.xephi.authme.permission.AuthGroupType;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.service.ValidationService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;

/**
 * Service for asynchronous and synchronous processes.
 */
public class ProcessService {

    @Inject
    private Settings settings;

    @Inject
    private Messages messages;

    @Inject
    private ValidationService validationService;

    @Inject
    private PermissionsManager permissionsManager;

    @Inject
    private AuthGroupHandler authGroupHandler;

    /**
     * Retrieve a property's value.
     *
     * @param property the property to retrieve
     * @param <T> the property type
     * @return the property's value
     */
    public <T> T getProperty(Property<T> property) {
        return settings.getProperty(property);
    }

    /**
     * Return the settings manager.
     *
     * @return settings manager
     */
    public Settings getSettings() {
        return settings;
    }

    /**
     * Send a message to the command sender.
     *
     * @param sender the command sender
     * @param key the message key
     */
    public void send(CommandSender sender, MessageKey key) {
        messages.send(sender, key);
    }

    /**
     * Send a message to the command sender with the given replacements.
     *
     * @param sender the command sender
     * @param key the message key
     * @param replacements the replacements to apply to the message
     */
    public void send(CommandSender sender, MessageKey key, String... replacements) {
        messages.send(sender, key, replacements);
    }

    /**
     * Retrieve a message.
     *
     * @param key the key of the message
     * @return the message, split by line
     */
    public String[] retrieveMessage(MessageKey key) {
        return messages.retrieve(key);
    }

    /**
     * Retrieve a message as one piece.
     *
     * @param key the key of the message
     * @return the message
     */
    public String retrieveSingleMessage(MessageKey key) {
        return messages.retrieveSingle(key);
    }

    public boolean validateEmail(String email) {
        return validationService.validateEmail(email);
    }

    public boolean isEmailFreeForRegistration(String email, CommandSender sender) {
        return validationService.isEmailFreeForRegistration(email, sender);
    }

    public boolean hasPermission(Player player, PermissionNode node) {
        return permissionsManager.hasPermission(player, node);
    }

    public boolean setGroup(Player player, AuthGroupType group) {
        return authGroupHandler.setGroup(player, group);
    }

}
