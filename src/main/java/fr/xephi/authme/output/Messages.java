package fr.xephi.authme.output;

import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.command.CommandSender;

/**
 * Class for retrieving and sending translatable messages to players.
 * This class detects when the language settings have changed and will
 * automatically update to use a new language file.
 */
public class Messages {

    private static Messages singleton;
    private final String language;
    private MessagesManager manager;


    private Messages(String language, MessagesManager manager) {
        this.language = language;
        this.manager = manager;
    }

    /**
     * Get the instance of Messages.
     *
     * @return The Messages instance
     */
    public static Messages getInstance() {
        if (singleton == null) {
            MessagesManager manager = new MessagesManager(Settings.messageFile);
            singleton = new Messages(Settings.messagesLanguage, manager);
        }
        return singleton;
    }

    /**
     * Send the given message code to the player.
     *
     * @param sender The entity to send the message to
     * @param key The key of the message to send
     */
    public void send(CommandSender sender, MessageKey key) {
        String[] lines = manager.retrieve(key.getKey());
        for (String line : lines) {
            sender.sendMessage(line);
        }
    }

    /**
     * Send the given message code to the player with the given tag replacements. Note that this method
     * issues an exception if the number of supplied replacements doesn't correspond to the number of tags
     * the message key contains.
     *
     * @param sender The entity to send the message to
     * @param key The key of the message to send
     * @param replacements The replacements to apply for the tags
     */
    public void send(CommandSender sender, MessageKey key, String... replacements) {
        String message = retrieveSingle(key);
        String[] tags = key.getTags();
        if (replacements.length != tags.length) {
            throw new RuntimeException("Given replacement size does not match the tags in message key '" + key + "'");
        }

        for (int i = 0; i < tags.length; ++i) {
            message = message.replace(tags[i], replacements[i]);
        }

        for (String line : message.split("\n")) {
            sender.sendMessage(line);
        }
    }

    /**
     * Retrieve the message from the text file and return it split by new line as an array.
     *
     * @param key The message key to retrieve
     *
     * @return The message split by new lines
     */
    public String[] retrieve(MessageKey key) {
        if (!Settings.messagesLanguage.equalsIgnoreCase(language)) {
            reloadManager();
        }
        return manager.retrieve(key.getKey());
    }

    /**
     * Retrieve the message from the text file.
     *
     * @param key The message key to retrieve
     *
     * @return The message from the file
     */
    public String retrieveSingle(MessageKey key) {
        return StringUtils.join("\n", retrieve(key));
    }

    /**
     * Reload the messages manager.
     */
    public void reloadManager() {
        manager = new MessagesManager(Settings.messageFile);
    }

}
