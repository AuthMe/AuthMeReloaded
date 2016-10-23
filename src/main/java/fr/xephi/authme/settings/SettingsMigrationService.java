package fr.xephi.authme.settings;

import com.github.authme.configme.migration.PlainMigrationService;
import com.github.authme.configme.properties.Property;
import com.github.authme.configme.resource.PropertyResource;
import com.google.common.base.Objects;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.output.LogLevel;
import fr.xephi.authme.settings.properties.PluginSettings;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static com.github.authme.configme.properties.PropertyInitializer.newListProperty;
import static com.github.authme.configme.properties.PropertyInitializer.newProperty;
import static fr.xephi.authme.settings.properties.RegistrationSettings.DELAY_JOIN_MESSAGE;
import static fr.xephi.authme.settings.properties.RegistrationSettings.REMOVE_JOIN_MESSAGE;
import static fr.xephi.authme.settings.properties.RegistrationSettings.REMOVE_LEAVE_MESSAGE;
import static fr.xephi.authme.settings.properties.RestrictionSettings.ALLOWED_NICKNAME_CHARACTERS;
import static fr.xephi.authme.settings.properties.RestrictionSettings.FORCE_SPAWN_LOCATION_AFTER_LOGIN;
import static fr.xephi.authme.settings.properties.RestrictionSettings.FORCE_SPAWN_ON_WORLDS;

/**
 * Service for verifying that the configuration is up-to-date.
 */
public class SettingsMigrationService extends PlainMigrationService {

    private final File pluginFolder;

    public SettingsMigrationService(File pluginFolder) {
        this.pluginFolder = pluginFolder;
    }

    @Override
    protected boolean performMigrations(PropertyResource resource, List<Property<?>> properties) {
        boolean changes = false;
        if ("[a-zA-Z0-9_?]*".equals(resource.getString(ALLOWED_NICKNAME_CHARACTERS.getPath()))) {
            resource.setValue(ALLOWED_NICKNAME_CHARACTERS.getPath(), "[a-zA-Z0-9_]*");
            changes = true;
        }

        // Note ljacqu 20160211: Concatenating migration methods with | instead of the usual ||
        // ensures that all migrations will be performed
        return changes
            | performMailTextToFileMigration(resource)
            | migrateJoinLeaveMessages(resource)
            | migrateForceSpawnSettings(resource)
            | changeBooleanSettingToLogLevelProperty(resource)
            | hasOldHelpHeaderProperty(resource)
            || hasDeprecatedProperties(resource);
    }

    private static boolean hasDeprecatedProperties(PropertyResource resource) {
        String[] deprecatedProperties = {
            "Converter.Rakamak.newPasswordHash", "Hooks.chestshop", "Hooks.legacyChestshop", "Hooks.notifications",
            "Passpartu", "Performances", "settings.restrictions.enablePasswordVerifier", "Xenoforo.predefinedSalt",
            "VeryGames", "settings.restrictions.allowAllCommandsIfRegistrationIsOptional", "DataSource.mySQLWebsite",
            "Hooks.customAttributes", "Security.stop.kickPlayersBeforeStopping"};
        for (String deprecatedPath : deprecatedProperties) {
            if (resource.contains(deprecatedPath)) {
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
     * @param resource The property resource
     * @return True if a migration has been completed, false otherwise
     */
    private boolean performMailTextToFileMigration(PropertyResource resource) {
        final String oldSettingPath = "Email.mailText";
        final String oldMailText = resource.getString(oldSettingPath);
        if (oldMailText == null) {
            return false;
        }

        final File emailFile = new File(pluginFolder, "email.html");
        final String mailText = oldMailText
            .replace("<playername>", "<playername />").replace("%playername%", "<playername />")
            .replace("<servername>", "<servername />").replace("%servername%", "<servername />")
            .replace("<generatedpass>", "<generatedpass />").replace("%generatedpass%", "<generatedpass />")
            .replace("<image>", "<image />").replace("%image%", "<image />");
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
     * @param resource The property resource
     * @return True if the configuration has changed, false otherwise
     */
    private static boolean migrateJoinLeaveMessages(PropertyResource resource) {
        Property<Boolean> oldDelayJoinProperty = newProperty("settings.delayJoinLeaveMessages", false);
        boolean hasMigrated = moveProperty(oldDelayJoinProperty, DELAY_JOIN_MESSAGE, resource);

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
     * @param resource The property resource
     * @return True if the configuration has changed, false otherwise
     */
    private static boolean migrateForceSpawnSettings(PropertyResource resource) {
        Property<Boolean> oldForceLocEnabled = newProperty(
            "settings.restrictions.ForceSpawnLocOnJoinEnabled", false);
        Property<List<String>> oldForceWorlds = newListProperty(
            "settings.restrictions.ForceSpawnOnTheseWorlds", "world", "world_nether", "world_the_ed");

        return moveProperty(oldForceLocEnabled, FORCE_SPAWN_LOCATION_AFTER_LOGIN, resource)
            | moveProperty(oldForceWorlds, FORCE_SPAWN_ON_WORLDS, resource);
    }

    /**
     * Changes the old boolean property "hide spam from console" to the new property specifying
     * the log level.
     *
     * @param resource The property resource
     * @return True if the configuration has changed, false otherwise
     */
    private static boolean changeBooleanSettingToLogLevelProperty(PropertyResource resource) {
        final String oldPath = "Security.console.noConsoleSpam";
        final Property<LogLevel> newProperty = PluginSettings.LOG_LEVEL;
        if (!newProperty.isPresent(resource) && resource.contains(oldPath)) {
            ConsoleLogger.info("Moving '" + oldPath + "' to '" + newProperty.getPath() + "'");
            boolean oldValue = Objects.firstNonNull(resource.getBoolean(oldPath), false);
            LogLevel level = oldValue ? LogLevel.INFO : LogLevel.FINE;
            resource.setValue(newProperty.getPath(), level.name());
            return true;
        }
        return false;
    }

    private static boolean hasOldHelpHeaderProperty(PropertyResource resource) {
        if (resource.contains("settings.helpHeader")) {
            ConsoleLogger.warning("Help header setting is now in messages/help_xx.yml, "
                + "please check the file to set it again");
            return true;
        }
        return false;
    }

    /**
     * Checks for an old property path and moves it to a new path if present.
     *
     * @param oldProperty The old property (create a temporary {@link Property} object with the path)
     * @param newProperty The new property to move the value to
     * @param resource The property resource
     * @param <T> The type of the property
     * @return True if a migration has been done, false otherwise
     */
    private static <T> boolean moveProperty(Property<T> oldProperty,
                                            Property<T> newProperty,
                                            PropertyResource resource) {
        if (resource.contains(oldProperty.getPath())) {
            ConsoleLogger.info("Detected deprecated property " + oldProperty.getPath());
            if (!resource.contains(newProperty.getPath())) {
                ConsoleLogger.info("Renamed " + oldProperty.getPath() + " to " + newProperty.getPath());
                resource.setValue(newProperty.getPath(), oldProperty.getValue(resource));
            }
            return true;
        }
        return false;
    }

}
