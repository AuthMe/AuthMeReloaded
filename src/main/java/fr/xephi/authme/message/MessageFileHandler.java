package fr.xephi.authme.message;

import fr.xephi.authme.ConsoleLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Handles a YAML message file with a default file fallback.
 */
public class MessageFileHandler {

    // regular file
    private final String filename;
    private final FileConfiguration configuration;
    // default file
    private final String defaultFile;
    private FileConfiguration defaultConfiguration;

    /**
     * Constructor.
     *
     * @param file the file to use for messages
     * @param defaultFile the default file from the JAR to use if no message is found
     */
    public MessageFileHandler(File file, String defaultFile) {
        this.filename = file.getName();
        this.configuration = YamlConfiguration.loadConfiguration(file);
        this.defaultFile = defaultFile;
    }

    /**
     * Returns whether the message file configuration has an entry at the given path.
     *
     * @param path the path to verify
     * @return true if an entry exists for the path in the messages file, false otherwise
     */
    public boolean hasSection(String path) {
        return configuration.get(path) != null;
    }

    /**
     * Returns the message for the given key.
     *
     * @param key the key to retrieve the message for
     * @return the message
     */
    public String getMessage(String key) {
        String message = configuration.getString(key);

        if (message == null) {
            ConsoleLogger.warning("Error getting message with key '" + key + "'. "
                + "Please verify your config file at '" + filename + "'");
            return getDefault(key);
        }
        return message;
    }

    /**
     * Returns the message for the given key only if it exists,
     * i.e. without falling back to the default file.
     *
     * @param key the key to retrieve the message for
     * @return the message, or {@code null} if not available
     */
    public String getMessageIfExists(String key) {
        return configuration.getString(key);
    }

    /**
     * Gets the message from the default file.
     *
     * @param key the key to retrieve the message for
     * @return the message from the default file
     */
    private String getDefault(String key) {
        if (defaultConfiguration == null) {
            InputStream stream = MessageFileHandler.class.getClassLoader().getResourceAsStream(defaultFile);
            defaultConfiguration = YamlConfiguration.loadConfiguration(new InputStreamReader(stream));
        }
        String message = defaultConfiguration.getString(key);
        return message == null
            ? "Error retrieving message '" + key + "'"
            : message;
    }
}
