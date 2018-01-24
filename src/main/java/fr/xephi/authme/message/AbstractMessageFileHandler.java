package fr.xephi.authme.message;

import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.util.FileUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Handles a YAML message file with a default file fallback.
 */
public abstract class AbstractMessageFileHandler implements Reloadable {

    protected static final String DEFAULT_LANGUAGE = "en";

    @DataFolder
    @Inject
    private File dataFolder;

    @Inject
    private Settings settings;

    private String filename;
    private FileConfiguration configuration;
    private final String defaultFile;
    private FileConfiguration defaultConfiguration;
    private final String updateCommandAddition;

    protected AbstractMessageFileHandler() {
        this.defaultFile = createFilePath(DEFAULT_LANGUAGE);
        String updateCommand = getUpdateCommand();
        this.updateCommandAddition = updateCommand == null
            ? ""
            : " or run " + updateCommand;
    }

    @Override
    @PostConstruct
    public void reload() {
        String language = getLanguage();
        filename = createFilePath(language);
        File messagesFile = initializeFile(filename);
        configuration = YamlConfiguration.loadConfiguration(messagesFile);
    }

    protected String getLanguage() {
        return settings.getProperty(PluginSettings.MESSAGES_LANGUAGE);
    }

    protected File getUserLanguageFile() {
        return new File(dataFolder, filename);
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
                + "Please update your config file '" + filename + "'" + updateCommandAddition);
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
            InputStream stream = FileUtils.getResourceFromJar(defaultFile);
            defaultConfiguration = YamlConfiguration.loadConfiguration(new InputStreamReader(stream));
        }
        String message = defaultConfiguration.getString(key);
        return message == null
            ? "Error retrieving message '" + key + "'"
            : message;
    }

    /**
     * Creates the path to the messages file for the given language code.
     *
     * @param language the language code
     * @return path to the message file for the given language
     */
    protected abstract String createFilePath(String language);

    /**
     * @return command with which the messages file can be updated; output when a message is missing from the file
     */
    protected abstract String getUpdateCommand();

    /**
     * Copies the messages file from the JAR to the local messages/ folder if it doesn't exist.
     *
     * @param filePath path to the messages file to use
     * @return the messages file to use
     */
    @VisibleForTesting
    File initializeFile(String filePath) {
        File file = new File(dataFolder, filePath);
        // Check that JAR file exists to avoid logging an error
        if (FileUtils.getResourceFromJar(filePath) != null && FileUtils.copyFileFromResource(file, filePath)) {
            return file;
        }

        if (FileUtils.copyFileFromResource(file, defaultFile)) {
            return file;
        } else {
            ConsoleLogger.warning("Wanted to copy default messages file '" + defaultFile
                + "' from JAR but it didn't exist");
            return null;
        }
    }
}
