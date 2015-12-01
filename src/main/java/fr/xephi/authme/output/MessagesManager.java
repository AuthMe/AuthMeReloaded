package fr.xephi.authme.output;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.CustomConfiguration;

import java.io.File;

/**
 * Class for retrieving and sending translatable messages to players.
 */
class MessagesManager extends CustomConfiguration {

    /** The section symbol, used in Minecraft for formatting codes. */
    private static final String SECTION_SIGN = "\u00a7";


    /**
     * Constructor for Messages.
     *
     * @param file the configuration file
     */
    MessagesManager(File file) {
        super(file);
        load();
    }

    /**
     * Retrieve the message from the configuration file.
     *
     * @param key The key to retrieve
     *
     * @return The message
     */
    String[] retrieve(String key) {
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

    static String[] formatMessage(String message) {
        // TODO: Check that the codes actually exist, i.e. replace &c but not &y
        // TODO: Allow '&' to be retained with the code '&&'
        String[] lines = message.split("&n");
        for (int i = 0; i < lines.length; ++i) {
            // We don't initialize a StringBuilder here because mostly we will only have one entry
            lines[i] = lines[i].replace("&", SECTION_SIGN);
        }
        return lines;
    }

}
