package fr.xephi.authme.settings;

import com.github.authme.configme.SettingsManager;
import com.github.authme.configme.knownproperties.PropertyEntry;
import com.github.authme.configme.migration.MigrationService;
import com.github.authme.configme.resource.PropertyResource;
import com.google.common.io.Files;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static fr.xephi.authme.util.FileUtils.copyFileFromResource;

/**
 * The AuthMe settings manager.
 */
public class Settings extends SettingsManager {

    private final File pluginFolder;
    /** The file with the localized messages based on {@link PluginSettings#MESSAGES_LANGUAGE}. */
    private File messagesFile;
    private String[] welcomeMessage;
    private String passwordEmailMessage;
    private String recoveryCodeEmailMessage;

    /**
     * Constructor.
     *
     * @param pluginFolder the AuthMe plugin folder
     * @param resource the property resource to read and write properties to
     * @param migrationService migration service to check the settings file with
     * @param knownProperties collection of all available settings
     */
    public Settings(File pluginFolder, PropertyResource resource, MigrationService migrationService,
                    List<PropertyEntry> knownProperties) {
        super(resource, migrationService, knownProperties);
        this.pluginFolder = pluginFolder;
        loadSettingsFromFiles();
    }

    /**
     * Return the messages file based on the messages language config.
     *
     * @return The messages file to read messages from
     */
    public File getMessagesFile() {
        return messagesFile;
    }

    /**
     * Return the path to the default messages file within the JAR.
     *
     * @return The default messages file path
     */
    public String getDefaultMessagesFile() {
        return "/messages/messages_en.yml";
    }

    /**
     * Return the text to use in email registrations.
     *
     * @return The email message
     */
    public String getPasswordEmailMessage() {
        return passwordEmailMessage;
    }

    /**
     * Return the text to use when someone requests to receive a recovery code.
     *
     * @return The email message
     */
    public String getRecoveryCodeEmailMessage() {
        return recoveryCodeEmailMessage;
    }

    /**
     * Return the lines to output after an in-game registration.
     *
     * @return The welcome message
     */
    public String[] getWelcomeMessage() {
        return welcomeMessage;
    }

    private void loadSettingsFromFiles() {
        messagesFile = buildMessagesFile();
        passwordEmailMessage = readFile("email.html");
        recoveryCodeEmailMessage = readFile("recovery_code_email.html");
        welcomeMessage = readFile("welcome.txt").split("\n");
    }

    @Override
    public void reload() {
        super.reload();
        loadSettingsFromFiles();
    }

    private File buildMessagesFile() {
        String languageCode = getProperty(PluginSettings.MESSAGES_LANGUAGE);

        String filePath = buildMessagesFilePathFromCode(languageCode);
        File messagesFile = new File(pluginFolder, filePath);
        if (copyFileFromResource(messagesFile, filePath)) {
            return messagesFile;
        }

        // File doesn't exist or couldn't be copied - try again with default, "en"
        String defaultFilePath = buildMessagesFilePathFromCode("en");
        File defaultFile = new File(pluginFolder, defaultFilePath);
        copyFileFromResource(defaultFile, defaultFilePath);

        // No matter the result, need to return a file
        return defaultFile;
    }

    private static String buildMessagesFilePathFromCode(String language) {
        return StringUtils.makePath("messages", "messages_" + language + ".yml");
    }

    /**
     * Reads a file from the plugin folder or copies it from the JAR to the plugin folder.
     *
     * @param filename the file to read
     * @return the file's contents
     */
    private String readFile(String filename) {
        final File file = new File(pluginFolder, filename);
        if (copyFileFromResource(file, filename)) {
            try {
                return Files.toString(file, StandardCharsets.UTF_8);
            } catch (IOException e) {
                ConsoleLogger.logException("Failed to read file '" + filename + "':", e);
            }
        } else {
            ConsoleLogger.warning("Failed to copy file '" + filename + "' from JAR");
        }
        return "";
    }
}
