package fr.xephi.authme.message;

import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.util.FileUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import static fr.xephi.authme.message.MessagePathHelper.DEFAULT_LANGUAGE;

/**
 * Handles a YAML message file with a default file fallback.
 */
public abstract class AbstractMessageFileHandler implements Reloadable {

    private static final FileConfiguration UNAVAILABLE = new YamlConfiguration();

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(AbstractMessageFileHandler.class);

    @DataFolder
    @Inject
    private File dataFolder;

    @Inject
    private Settings settings;

    private String filename;
    private FileConfiguration configuration;
    private final String defaultFile;
    private final ConcurrentHashMap<String, FileConfiguration> languageCache = new ConcurrentHashMap<>();

    protected AbstractMessageFileHandler() {
        this.defaultFile = createFilePath(DEFAULT_LANGUAGE);
    }

    @Override
    @PostConstruct
    public void reload() {
        String language = settings.getProperty(PluginSettings.MESSAGES_LANGUAGE);
        filename = createFilePath(language);
        File messagesFile = initializeFile(filename);
        configuration = YamlConfiguration.loadConfiguration(messagesFile);
        languageCache.clear();
    }

    protected String getLanguage() {
        return settings.getProperty(PluginSettings.MESSAGES_LANGUAGE);
    }

    protected File getUserLanguageFile() {
        return new File(dataFolder, filename);
    }

    protected String getFilename() {
        return filename;
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
        return message == null
            ? "Error retrieving message '" + key + "'"
            : message;
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
     * Returns the message for the given key in the requested language, falling back to the
     * server-configured language if the requested language is unavailable or missing the key.
     *
     * @param key the key to retrieve the message for
     * @param language the AuthMe language code (e.g. {@code "fr"}), or {@code null} to use the server default
     * @return the message
     */
    public String getMessage(String key, String language) {
        if (language == null || language.equals(getLanguage())) {
            return getMessage(key);
        }
        FileConfiguration config = languageCache.computeIfAbsent(language, this::loadLanguageConfiguration);
        if (config == UNAVAILABLE) {
            return getMessage(key);
        }
        String message = config.getString(key);
        return message != null ? message : getMessage(key);
    }

    private FileConfiguration loadLanguageConfiguration(String language) {
        String filePath = createFilePath(language);
        File file = new File(dataFolder, filePath);
        if (!file.exists() && FileUtils.getResourceFromJar(filePath) != null) {
            FileUtils.copyFileFromResource(file, filePath);
        }
        return file.exists() ? YamlConfiguration.loadConfiguration(file) : UNAVAILABLE;
    }

    /**
     * Creates the path to the messages file for the given language code.
     *
     * @param language the language code
     * @return path to the message file for the given language
     */
    protected abstract String createFilePath(String language);

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
            logger.warning("Wanted to copy default messages file '" + defaultFile + "' from JAR but it didn't exist");
            return null;
        }
    }
}
