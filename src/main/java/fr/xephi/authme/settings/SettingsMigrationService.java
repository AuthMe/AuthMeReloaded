package fr.xephi.authme.settings;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.propertymap.PropertyMap;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static fr.xephi.authme.settings.properties.RegistrationSettings.DELAY_JOIN_MESSAGE;
import static fr.xephi.authme.settings.properties.RegistrationSettings.REMOVE_JOIN_MESSAGE;
import static fr.xephi.authme.settings.properties.RegistrationSettings.REMOVE_LEAVE_MESSAGE;
import static fr.xephi.authme.settings.properties.RestrictionSettings.ALLOWED_NICKNAME_CHARACTERS;
import static fr.xephi.authme.settings.properties.RestrictionSettings.FORCE_SPAWN_LOCATION_AFTER_LOGIN;
import static fr.xephi.authme.settings.properties.RestrictionSettings.FORCE_SPAWN_ON_WORLDS;

/**
 * Service for verifying that the configuration is up-to-date.
 */
public class SettingsMigrationService {

    /**
     * Checks the config file and performs any necessary migrations.
     *
     * @param configuration The file configuration to check and migrate
     * @param propertyMap The property map of all existing properties
     * @param pluginFolder The plugin folder
     * @return True if there is a change and the config must be saved, false if the config is up-to-date
     */
    public boolean checkAndMigrate(FileConfiguration configuration, PropertyMap propertyMap, File pluginFolder) {
        return performMigrations(configuration, pluginFolder)
            || hasDeprecatedProperties(configuration)
            || !containsAllSettings(configuration, propertyMap);
    }

    private boolean performMigrations(FileConfiguration configuration, File pluginFolder) {
        boolean changes = false;
        if ("[a-zA-Z0-9_?]*".equals(configuration.getString(ALLOWED_NICKNAME_CHARACTERS.getPath()))) {
            configuration.set(ALLOWED_NICKNAME_CHARACTERS.getPath(), "[a-zA-Z0-9_]*");
            changes = true;
        }

        // Note ljacqu 20160211: Concatenating migration methods with | instead of the usual ||
        // ensures that all migrations will be performed
        return changes
            | performMailTextToFileMigration(configuration, pluginFolder)
            | migrateJoinLeaveMessages(configuration)
            | migrateForceSpawnSettings(configuration);
    }

    public boolean containsAllSettings(FileConfiguration configuration, PropertyMap propertyMap) {
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
            "Passpartu", "Performances", "settings.restrictions.enablePasswordVerifier", "Xenoforo.predefinedSalt",
            "VeryGames", "settings.restrictions.allowAllCommandsIfRegistrationIsOptional"};
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
     * @param pluginFolder The plugin data folder
     * @return True if a migration has been completed, false otherwise
     */
    private static boolean performMailTextToFileMigration(FileConfiguration configuration, File pluginFolder) {
        final String oldSettingPath = "Email.mailText";
        if (!configuration.contains(oldSettingPath)) {
            return false;
        }

        final File emailFile = new File(pluginFolder, "email.html");
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
        Property<Boolean> oldDelayJoinProperty = Property.newProperty("settings.delayJoinLeaveMessages", false);
        boolean hasMigrated = moveProperty(oldDelayJoinProperty, DELAY_JOIN_MESSAGE, configuration);

        if (hasMigrated) {
            ConsoleLogger.info(String.format("Note that we now also have the settings %s and %s",
                REMOVE_JOIN_MESSAGE.getPath(), REMOVE_LEAVE_MESSAGE.getPath()));
        }
        return hasMigrated;
    }

    /**
     * Detect old "force spawn loc on join" and "force spawn on these worlds" settings and moves them
     * to the new paths.
     *
     * @param configuration The file configuration
     * @return True if the configuration has changed, false otherwise
     */
    private static boolean migrateForceSpawnSettings(FileConfiguration configuration) {
        Property<Boolean> oldForceLocEnabled = Property.newProperty(
            "settings.restrictions.ForceSpawnLocOnJoinEnabled", false);
        Property<List<String>> oldForceWorlds = Property.newListProperty(
            "settings.restrictions.ForceSpawnOnTheseWorlds", "world", "world_nether", "world_the_ed");

        return moveProperty(oldForceLocEnabled, FORCE_SPAWN_LOCATION_AFTER_LOGIN, configuration)
            | moveProperty(oldForceWorlds, FORCE_SPAWN_ON_WORLDS, configuration);
    }

    /**
     * Checks for an old property path and moves it to a new path if present.
     *
     * @param oldProperty The old property (create a temporary {@link Property} object with the path)
     * @param newProperty The new property to move the value to
     * @param configuration The file configuration
     * @param <T> The type of the property
     * @return True if a migration has been done, false otherwise
     */
    private static <T> boolean moveProperty(Property<T> oldProperty,
                                            Property<T> newProperty,
                                            FileConfiguration configuration) {
        if (configuration.contains(oldProperty.getPath())) {
            ConsoleLogger.info("Detected deprecated property " + oldProperty.getPath());
            if (!configuration.contains(newProperty.getPath())) {
                ConsoleLogger.info("Renamed " + oldProperty.getPath() + " to " + newProperty.getPath());
                configuration.set(newProperty.getPath(), oldProperty.getFromFile(configuration));
            }
            return true;
        }
        return false;
    }

}
