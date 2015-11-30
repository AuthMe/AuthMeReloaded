package fr.xephi.authme.settings;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.command.CommandSender;

import java.io.File;

/**
 * Class for retrieving and sending translatable messages to players.
 */
// TODO ljacqu 20151124: This class is a weird mix between singleton and POJO
// TODO: change it into POJO
public class Messages extends CustomConfiguration {

    /** The section symbol, used in Minecraft for formatting codes. */
    private static final String SECTION_SIGN = "\u00a7";
    private static Messages singleton;
    private String language;


    /**
     * Constructor for Messages.
     *
     * @param file the configuration file
     * @param lang the code of the language to use
     */
    public Messages(File file, String lang) {
        super(file);
        load();
        this.language = lang;
    }

    public static Messages getInstance() {
        if (singleton == null) {
            singleton = new Messages(Settings.messageFile, Settings.messagesLanguage);
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
        String[] lines = retrieve(key);
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
        return retrieve(key.getKey());
    }

    /**
     * Retrieve the message from the text file.
     *
     * @param key The message key to retrieve
     *
     * @return The message from the file
     */
    public String retrieveSingle(MessageKey key) {
        return StringUtils.join("\n", retrieve(key.getKey()));
    }

    /**
     * Retrieve the message from the configuration file.
     *
     * @param key The key to retrieve
     *
     * @return The message
     */
    private String[] retrieve(String key) {
        if (!Settings.messagesLanguage.equalsIgnoreCase(language)) {
            reloadMessages();
        }
        String message = (String) get(key);
        if (message != null) {
            return formatMessage(message);
        }

        // Message is null: log key not being found and send error back as message
        String retrievalError = "Error getting message with key '" + key + "'. ";
        ConsoleLogger.showError(retrievalError + "Please verify your config file at '"
            + getConfigFile().getName() + "'");
        return new String[]{
            retrievalError + "Please contact the admin to verify or update the AuthMe messages file."};
    }

    private static String[] formatMessage(String message) {
        // TODO: Check that the codes actually exist, i.e. replace &c but not &y
        // TODO: Allow '&' to be retained with the code '&&'
        String[] lines = message.split("&n");
        for (int i = 0; i < lines.length; ++i) {
            // We don't initialize a StringBuilder here because mostly we will only have one entry
            lines[i] = lines[i].replace("&", SECTION_SIGN);
        }
        return lines;
    }

    public void reloadMessages() {
        singleton = new Messages(Settings.messageFile, Settings.messagesLanguage);
    }

}
