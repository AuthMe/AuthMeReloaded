package fr.xephi.authme.message;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.Reloadable;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;

/**
 * Class for retrieving and sending translatable messages to players.
 */
public class Messages implements Reloadable {

    // Custom Authme tag replaced to new line
    private static final String NEWLINE_TAG = "%nl%";

    private final MessageFileHandlerProvider messageFileHandlerProvider;
    private MessageFileHandler messageFileHandler;

    /*
     * Constructor.
     */
    @Inject
    Messages(MessageFileHandlerProvider messageFileHandlerProvider) {
        this.messageFileHandlerProvider = messageFileHandlerProvider;
        reload();
    }

    /**
     * Send the given message code to the player.
     *
     * @param sender The entity to send the message to
     * @param key The key of the message to send
     */
    public void send(CommandSender sender, MessageKey key) {
        String[] lines = retrieve(key);
        for (String line : lines) {
            sender.sendMessage(line);
        }
    }

    /**
     * Send the given message code to the player with the given tag replacements. Note that this method
     * logs an error if the number of supplied replacements doesn't correspond to the number of tags
     * the message key contains.
     *
     * @param sender The entity to send the message to
     * @param key The key of the message to send
     * @param replacements The replacements to apply for the tags
     */
    public void send(CommandSender sender, MessageKey key, String... replacements) {
        String message = retrieveSingle(key, replacements);
        for (String line : message.split("\n")) {
            sender.sendMessage(line);
        }
    }

    /**
     * Retrieve the message from the text file and return it split by new line as an array.
     *
     * @param key The message key to retrieve
     * @return The message split by new lines
     */
    public String[] retrieve(MessageKey key) {
        String message = retrieveMessage(key);
        if (message.isEmpty()) {
            // Return empty array instead of array with 1 empty string as entry
            return new String[0];
        }
        return message.split("\n");
    }

    /**
     * Retrieve the message from the text file.
     *
     * @param key The message key to retrieve
     * @return The message from the file
     */
    private String retrieveMessage(MessageKey key) {
        return formatMessage(
            messageFileHandler.getMessage(key.getKey()));
    }

    /**
     * Retrieve the given message code with the given tag replacements. Note that this method
     * logs an error if the number of supplied replacements doesn't correspond to the number of tags
     * the message key contains.
     *
     * @param key The key of the message to send
     * @param replacements The replacements to apply for the tags
     * @return The message from the file with replacements
     */
    public String retrieveSingle(MessageKey key, String... replacements) {
        String message = retrieveMessage(key);
        String[] tags = key.getTags();
        if (replacements.length == tags.length) {
            for (int i = 0; i < tags.length; ++i) {
                message = message.replace(tags[i], replacements[i]);
            }
        } else {
            ConsoleLogger.warning("Invalid number of replacements for message key '" + key + "'");
        }
        return message;
    }

    @Override
    public void reload() {
        this.messageFileHandler = messageFileHandlerProvider
            .initializeHandler(lang -> "messages/messages_" + lang + ".yml");
    }

    private static String formatMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message)
            .replace(NEWLINE_TAG, "\n");
    }

}
