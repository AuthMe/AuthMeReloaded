package fr.xephi.authme.settings;

import com.github.authme.configme.SettingsManager;
import com.github.authme.configme.knownproperties.PropertyEntry;
import com.github.authme.configme.migration.MigrationService;
import com.github.authme.configme.resource.PropertyResource;
import com.google.common.io.Files;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static fr.xephi.authme.util.FileUtils.copyFileFromResource;

/**
 * The AuthMe settings manager.
 */
public class Settings extends SettingsManager {

    private final File pluginFolder;
    /** The file with the localized messages based on {@link PluginSettings#MESSAGES_LANGUAGE}. */
    private File messagesFile;
    private List<String> welcomeMessage;
    private String emailMessage;

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
    public String getEmailMessage() {
        return emailMessage;
    }

    /**
     * Return the lines to output after an in-game registration.
     *
     * @return The welcome message
     */
    public List<String> getWelcomeMessage() {
        return welcomeMessage;
    }

    private void loadSettingsFromFiles() {
        messagesFile = buildMessagesFile();
        welcomeMessage = readWelcomeMessage();
        emailMessage = readEmailMessage();
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

    private List<String> readWelcomeMessage() {
        if (getProperty(RegistrationSettings.USE_WELCOME_MESSAGE)) {
            final File welcomeFile = new File(pluginFolder, "welcome.txt");
            final Charset charset = Charset.forName("UTF-8");
            if (copyFileFromResource(welcomeFile, "welcome.txt")) {
                try {
                    return Files.readLines(welcomeFile, charset);
                } catch (IOException e) {
                    ConsoleLogger.logException("Failed to read file '" + welcomeFile.getPath() + "':", e);
                }
            }
        }
        return new ArrayList<>(0);
    }

    private String readEmailMessage() {
        final File emailFile = new File(pluginFolder, "email.html");
        final Charset charset = Charset.forName("UTF-8");
        if (copyFileFromResource(emailFile, "email.html")) {
            try {
                return Files.toString(emailFile, charset);
            } catch (IOException e) {
                ConsoleLogger.logException("Failed to read file '" + emailFile.getPath() + "':", e);
            }
        }
        return "";
    }
}
