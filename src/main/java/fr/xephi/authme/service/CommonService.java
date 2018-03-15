package fr.xephi.authme.service;

import ch.jalu.configme.properties.Property;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;

/**
 * Service for the most common operations regarding settings, messages and permissions.
 */
public class CommonService {

    @Inject
    private Settings settings;

    @Inject
    private Messages messages;

    @Inject
    private PermissionsManager permissionsManager;

    CommonService() {
    }

    /**
     * Retrieves a property's value.
     *
     * @param property the property to retrieve
     * @param <T> the property type
     * @return the property's value
     */
    public <T> T getProperty(Property<T> property) {
        return settings.getProperty(property);
    }

    /**
     * Sends a message to the command sender.
     *
     * @param sender the command sender
     * @param key the message key
     */
    public void send(CommandSender sender, MessageKey key) {
        messages.send(sender, key);
    }

    /**
     * Sends a message to the command sender with the given replacements.
     *
     * @param sender the command sender
     * @param key the message key
     * @param replacements the replacements to apply to the message
     */
    public void send(CommandSender sender, MessageKey key, String... replacements) {
        messages.send(sender, key, replacements);
    }

    /**
     * Retrieves a message in one piece.
     *
     * @param sender The entity to send the message to
     * @param key the key of the message
     * @return the message
     */
    public String retrieveSingleMessage(CommandSender sender, MessageKey key) {
        return messages.retrieveSingle(sender, key);
    }

    /**
     * Checks whether the player has the given permission.
     *
     * @param player the player
     * @param node the permission node to check
     * @return true if player has permission, false otherwise
     */
    public boolean hasPermission(Player player, PermissionNode node) {
        return permissionsManager.hasPermission(player, node);
    }
}
