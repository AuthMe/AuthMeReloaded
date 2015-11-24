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
     * Sends the given message code to the player.
     *
     * @param sender The entity to send the message to
     * @param msg The message code to send
     */
    public void send(CommandSender sender, String msg) {
        if (!Settings.messagesLanguage.equalsIgnoreCase(singleton.language)) {
            singleton.reloadMessages();
        }
        String loc = (String) singleton.get(msg);
        if (loc == null) {
            loc = "Error with Translation files, please contact the admin for verify or update translation";
            ConsoleLogger.showError("Error with the " + msg + " translation, verify in your " + getConfigFile() + " !");
        }
        for (String l : loc.split("&n")) {
            sender.sendMessage(l.replace("&", SECTION_SIGN));
        }
    }

    public String[] send(MessageKey key) {
        return send(key.getKey());
    }

    /**
     * Retrieve the message from the text file and returns it split by new line as an array.
     *
     * @param msg The message code to retrieve
     *
     * @return The message split by new lines
     */
    public String[] send(String msg) {
        String s = retrieveMessage(msg);
        int i = s.split("&n").length;
        String[] loc = new String[i];
        int a;
        for (a = 0; a < i; a++) {
            loc[a] = s.split("&n")[a].replace("&", SECTION_SIGN);
        }
        if (loc.length == 0) {
            loc[0] = "Error with " + msg + " translation; Please contact the admin for verify or update translation files";
        }
        return loc;
    }

    /**
     * Retrieve the message from the configuration file.
     *
     * @param key The key to retrieve
     *
     * @return The message
     */
    private static String retrieveMessage(String key) {
        if (!Settings.messagesLanguage.equalsIgnoreCase(singleton.language)) {
            singleton.reloadMessages();
        }
        String message = (String) singleton.get(key);
        if (message != null) {
            return message;
        }

        // Message is null: log key not being found and send error back as message
        String retrievalError = "Error getting message with key '" + key + "'. ";
        ConsoleLogger.showError(retrievalError + "Please verify your config file at '"
            + singleton.getConfigFile().getName() + "'");
        return retrievalError + "Please contact the admin to verify or update the AuthMe messages file.";
    }

    private static String formatChatCodes(String message) {
        // TODO: Check that the codes actually exist, i.e. replace &c but not &y
        // TODO: Allow '&' to be retained with the code '&&'
        return message.replace("&", SECTION_SIGN);
    }

    public void reloadMessages() {
        singleton = new Messages(Settings.messageFile, Settings.messagesLanguage);
    }

}
