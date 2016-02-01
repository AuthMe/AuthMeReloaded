package fr.xephi.authme.output;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.CustomConfiguration;
import org.bukkit.ChatColor;

import java.io.File;

/**
 * Class responsible for reading messages from a file and formatting them for Minecraft.
 * <p>
 * This class is used within {@link Messages}, which offers a high-level interface for accessing
 * or sending messages from a properties file.
 */
class MessagesManager extends CustomConfiguration {

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
        String[] lines = message.split("&n");
        for (int i = 0; i < lines.length; ++i) {
            // We don't initialize a StringBuilder here because mostly we will only have one entry
            lines[i] = ChatColor.translateAlternateColorCodes('&', lines[i]);
        }
        return lines;
    }

}
