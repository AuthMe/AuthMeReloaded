package fr.xephi.authme.settings;

import fr.xephi.authme.ConsoleLogger;
import org.bukkit.command.CommandSender;

import java.io.File;

/**
 */
// TODO ljacqu 20151124: This class is a weird mix between singleton and POJO
public class Messages extends CustomConfiguration {

    /** The section symbol, used in Minecraft for formatting codes. */
    private static final String SECTION_SIGN = "\u00a7";
    private static Messages singleton = null;
    private String language = "en";


    /**
     * Constructor for Messages.
     *
     * @param file the configuration file
     * @param lang the code of the language to use
     */
    public Messages(File file, String lang) {
        super(file);
        load();
        singleton = this;
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
     * Send the given message code to the player.
     *
     * @param sender The entity to send the message to
     * @param msg The message code to send
     *
     * @deprecated Use {@link Messages#send(CommandSender, MessageKey)} instead
     */
    @Deprecated
    public void send(CommandSender sender, String msg) {
        String[] lines = retrieve(msg);
        for (String line : lines) {
            sender.sendMessage(line);
        }
    }

    public String[] retrieve(MessageKey key) {
        return retrieve(key.getKey());
    }

    /**
     * Retrieve the message from the text file and returns it split by new line as an array.
     *
     * @param msg The message code to retrieve
     *
     * @return The message split by new lines
     * @deprecated Use {@link Messages#retrieve(MessageKey)} instead.
     */
    @Deprecated
    public String[] send(String msg) {
        return retrieve(msg);
    }

    /**
     * Retrieve the message from the configuration file.
     *
     * @param key The key to retrieve
     *
     * @return The message
     */
    private static String[] retrieve(String key) {
        if (!Settings.messagesLanguage.equalsIgnoreCase(singleton.language)) {
            singleton.reloadMessages();
        }
        String message = (String) singleton.get(key);
        if (message != null) {
            return formatMessage(message);
        }

        // Message is null: log key not being found and send error back as message
        String retrievalError = "Error getting message with key '" + key + "'. ";
        ConsoleLogger.showError(retrievalError + "Please verify your config file at '"
            + singleton.getConfigFile().getName() + "'");
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
