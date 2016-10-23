package fr.xephi.authme.settings;

import com.github.authme.configme.SettingsManager;
import com.github.authme.configme.knownproperties.ConfigurationData;
import com.github.authme.configme.migration.MigrationService;
import com.github.authme.configme.resource.PropertyResource;
import com.google.common.io.Files;
import fr.xephi.authme.ConsoleLogger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static fr.xephi.authme.util.FileUtils.copyFileFromResource;

/**
 * The AuthMe settings manager.
 */
public class Settings extends SettingsManager {

    private final File pluginFolder;
    private String[] welcomeMessage;
    private String passwordEmailMessage;
    private String recoveryCodeEmailMessage;

    /**
     * Constructor.
     *
     * @param pluginFolder the AuthMe plugin folder
     * @param resource the property resource to read and write properties to
     * @param migrationService migration service to check the settings file with
     * @param configurationData configuration data (properties and comments)
     */
    public Settings(File pluginFolder, PropertyResource resource, MigrationService migrationService,
                    ConfigurationData configurationData) {
        super(resource, migrationService, configurationData);
        this.pluginFolder = pluginFolder;
        loadSettingsFromFiles();
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
        passwordEmailMessage = readFile("email.html");
        recoveryCodeEmailMessage = readFile("recovery_code_email.html");
        welcomeMessage = readFile("welcome.txt").split("\n");
    }

    @Override
    public void reload() {
        super.reload();
        loadSettingsFromFiles();
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
