package fr.xephi.authme.output;

import fr.xephi.authme.ConsoleLogger;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * Class responsible for reading messages from a file and formatting them for Minecraft.
 * <p>
 * This class is used within {@link Messages}, which offers a high-level interface for accessing
 * or sending messages from a properties file.
 */
class MessagesManager {

    private final YamlConfiguration configuration;
    private final String fileName;

    /**
     * Constructor for Messages.
     *
     * @param file the configuration file
     */
    MessagesManager(File file) {
        this.fileName = file.getName();
        this.configuration = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Retrieve the message from the configuration file.
     *
     * @param key The key to retrieve
     *
     * @return The message
     */
    public String[] retrieve(String key) {
        String message = configuration.getString(key);
        if (message != null) {
            return formatMessage(message);
        }

        // Message is null: log key not being found and send error back as message
        String retrievalError = "Error getting message with key '" + key + "'. ";
        ConsoleLogger.showError(retrievalError + "Please verify your config file at '" + fileName + "'");
        return new String[]{
            retrievalError + "Please contact the admin to verify or update the AuthMe messages file."};
    }

    private static String[] formatMessage(String message) {
        String[] lines = message.split("&n");
        for (int i = 0; i < lines.length; ++i) {
            lines[i] = ChatColor.translateAlternateColorCodes('&', lines[i]);
        }
        return lines;
    }

}
