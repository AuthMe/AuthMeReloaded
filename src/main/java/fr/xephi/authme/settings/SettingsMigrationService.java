package fr.xephi.authme.settings;

import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.propertymap.PropertyMap;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static fr.xephi.authme.settings.properties.RegistrationSettings.DELAY_JOIN_MESSAGE;
import static fr.xephi.authme.settings.properties.RegistrationSettings.REMOVE_JOIN_MESSAGE;
import static fr.xephi.authme.settings.properties.RegistrationSettings.REMOVE_LEAVE_MESSAGE;
import static fr.xephi.authme.settings.properties.RestrictionSettings.ALLOWED_NICKNAME_CHARACTERS;

/**
 * Service for verifying that the configuration is up-to-date.
 */
public final class SettingsMigrationService {

    private SettingsMigrationService() {
    }

    /**
     * Checks the config file and does any necessary migrations.
     *
     * @param configuration The file configuration to check and migrate
     * @param propertyMap The property map of all existing properties
     * @param pluginFolder The plugin folder
     * @return True if there is a change and the config must be saved, false if the config is up-to-date
     */
    public static boolean checkAndMigrate(FileConfiguration configuration, PropertyMap propertyMap, File pluginFolder) {
        return performMigrations(configuration, pluginFolder) || hasDeprecatedProperties(configuration)
            || !containsAllSettings(configuration, propertyMap);
    }

    private static boolean performMigrations(FileConfiguration configuration, File pluginFolder) {
        boolean changes = false;
        if ("[a-zA-Z0-9_?]*".equals(configuration.getString(ALLOWED_NICKNAME_CHARACTERS.getPath()))) {
            configuration.set(ALLOWED_NICKNAME_CHARACTERS.getPath(), "[a-zA-Z0-9_]*");
            changes = true;
        }

        // Note ljacqu 20160211: Concatenating migration methods with | instead of the usual ||
        // ensures that all migrations will be performed
        return changes
            | performMailTextToFileMigration(configuration, pluginFolder)
            | migrateJoinLeaveMessages(configuration);
    }

    @VisibleForTesting
    static boolean containsAllSettings(FileConfiguration configuration, PropertyMap propertyMap) {
        for (Property<?> property : propertyMap.keySet()) {
            if (!property.isPresent(configuration)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasDeprecatedProperties(FileConfiguration configuration) {
        String[] deprecatedProperties = {
            "Converter.Rakamak.newPasswordHash", "Hooks.chestshop", "Hooks.legacyChestshop", "Hooks.notifications",
            "Passpartu", "Performances", "settings.restrictions.enablePasswordVerifier", "Xenoforo.predefinedSalt"};
        for (String deprecatedPath : deprecatedProperties) {
            if (configuration.contains(deprecatedPath)) {
                return true;
            }
        }
        return false;
    }

    // --------
    // Specific migrations
    // --------

    /**
     * Check if {@code Email.mailText} is present and move it to the Email.html file if it doesn't exist yet.
     *
     * @param configuration The file configuration to verify
     * @param dataFolder The plugin data folder
     * @return True if a migration has been completed, false otherwise
     */
    private static boolean performMailTextToFileMigration(FileConfiguration configuration, File dataFolder) {
        final String oldSettingPath = "Email.mailText";
        if (!configuration.contains(oldSettingPath)) {
            return false;
        }

        final File emailFile = new File(dataFolder, "email.html");
        final String mailText = configuration.getString(oldSettingPath)
            .replace("<playername>", "<playername />")
            .replace("<servername>", "<servername />")
            .replace("<generatedpass>", "<generatedpass />")
            .replace("<image>", "<image />");
        if (!emailFile.exists()) {
            try (FileWriter fw = new FileWriter(emailFile)) {
                fw.write(mailText);
            } catch (IOException e) {
                ConsoleLogger.logException("Could not create email.html configuration file:", e);
            }
        }
        return true;
    }

    /**
     * Detect deprecated {@code settings.delayJoinLeaveMessages} and inform user of new "remove join messages"
     * and "remove leave messages" settings.
     *
     * @param configuration The file configuration
     * @return True if the configuration has changed, false otherwise
     */
    private static boolean migrateJoinLeaveMessages(FileConfiguration configuration) {
        final String oldDelayJoinPath = "settings.delayJoinLeaveMessages";
        if (configuration.contains(oldDelayJoinPath)) {
            ConsoleLogger.info("Detected deprecated property " + oldDelayJoinPath);
            ConsoleLogger.info(String.format("Note that we now also have the settings %s and %s",
                REMOVE_JOIN_MESSAGE.getPath(), REMOVE_LEAVE_MESSAGE.getPath()));
            if (!configuration.contains(DELAY_JOIN_MESSAGE.getPath())) {
                configuration.set(DELAY_JOIN_MESSAGE.getPath(), true);
                ConsoleLogger.info("Renamed " + oldDelayJoinPath + " to " + DELAY_JOIN_MESSAGE.getPath());
            }
            return true;
        }
        return false;
    }

}
