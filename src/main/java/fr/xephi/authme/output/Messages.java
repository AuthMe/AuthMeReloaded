package fr.xephi.authme.output;

import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.command.CommandSender;

/**
 * Class for retrieving and sending translatable messages to players.
 */
public class Messages {

    private static Messages singleton;
    private final String language;
    private MessagesManager manager;


    private Messages(String language, MessagesManager manager) {
        this.language = language;
        this.manager = manager;
    }

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

    public void reloadManager() {
        manager = new MessagesManager(Settings.messageFile);
    }

}
