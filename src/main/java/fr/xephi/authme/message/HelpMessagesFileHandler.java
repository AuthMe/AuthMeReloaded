package fr.xephi.authme.message;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.util.FileUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.inject.Inject;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * File handler for the help_xx.yml resource.
 */
public class HelpMessagesFileHandler extends AbstractMessageFileHandler {

    private FileConfiguration defaultConfiguration;

    @Inject // Trigger injection in the superclass
    HelpMessagesFileHandler() {
    }

    /**
     * Returns the message for the given key.
     *
     * @param key the key to retrieve the message for
     * @return the message
     */
    @Override
    public String getMessage(String key) {
        String message = getMessageIfExists(key);

        if (message == null) {
            ConsoleLogger.warning("Error getting message with key '" + key + "'. "
                + "Please update your config file '" + getFilename() + "' or run /authme messages help");
            return getDefault(key);
        }
        return message;
    }

    /**
     * Gets the message from the default file.
     *
     * @param key the key to retrieve the message for
     * @return the message from the default file
     */
    private String getDefault(String key) {
        if (defaultConfiguration == null) {
            InputStream stream = FileUtils.getResourceFromJar(createFilePath(DEFAULT_LANGUAGE));
            defaultConfiguration = YamlConfiguration.loadConfiguration(new InputStreamReader(stream));
        }
        String message = defaultConfiguration.getString(key);
        return message == null
            ? "Error retrieving message '" + key + "'"
            : message;
    }

    @Override
    protected String createFilePath(String language) {
        return "messages/help_" + language + ".yml";
    }
}
