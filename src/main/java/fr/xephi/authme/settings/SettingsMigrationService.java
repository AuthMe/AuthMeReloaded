package fr.xephi.authme.settings;

import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.propertymap.PropertyMap;
import fr.xephi.authme.util.StringUtils;
import fr.xephi.authme.util.Wrapper;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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
     * @return True if there is a change and the config must be saved, false if the config is up-to-date
     */
    public static boolean checkAndMigrate(FileConfiguration configuration, PropertyMap propertyMap) {
        return performMigrations(configuration) || hasDeprecatedProperties(configuration)
            || !containsAllSettings(configuration, propertyMap);
    }

    private static boolean performMigrations(FileConfiguration configuration) {
        boolean changes = false;
        if ("[a-zA-Z0-9_?]*".equals(configuration.getString(ALLOWED_NICKNAME_CHARACTERS.getPath()))) {
            configuration.set(ALLOWED_NICKNAME_CHARACTERS.getPath(), "[a-zA-Z0-9_]*");
            changes = true;
        }
        // TODO #450: Don't get the data folder statically
        Wrapper w = Wrapper.getInstance();
        changes = changes || performMailTextToFileMigration(configuration, w.getDataFolder());

        return changes;
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
            "Passpartu", "Performances", "settings.delayJoinMessage", "settings.restrictions.enablePasswordVerifier",
            "Xenoforo.predefinedSalt"};
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
        if (!emailFile.exists()) {
            try (FileWriter fw = new FileWriter(emailFile)) {
                fw.write(configuration.getString("Email.mailText"));
            } catch (IOException e) {
                ConsoleLogger.showError("Could not create email.html configuration file: "
                    + StringUtils.formatException(e));
            }
        }
        return true;
    }

}
