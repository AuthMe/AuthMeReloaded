package fr.xephi.authme.settings;

import fr.xephi.authme.settings.propertymap.PropertyMap;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

/**
 * Provides {@link SettingsMigrationService} implementations for testing.
 */
public final class TestSettingsMigrationServices {

    private TestSettingsMigrationServices() {
    }

    /**
     * Returns a settings migration service which always answers that all data is up-to-date.
     *
     * @return test settings migration service
     */
    public static SettingsMigrationService alwaysFulfilled() {
        return new SettingsMigrationService() {
            @Override
            public boolean checkAndMigrate(FileConfiguration configuration, PropertyMap propertyMap, File pluginFolder) {
                return false;
            }
            @Override
            public boolean containsAllSettings(FileConfiguration configuration, PropertyMap propertyMap) {
                return true;
            }
        };
    }

    /**
     * Returns a simple settings migration service which is fulfilled if all properties are present.
     *
     * @return test settings migration service
     */
    public static SettingsMigrationService checkAllPropertiesPresent() {
        return new SettingsMigrationService() {
            // See parent javadoc: true = some migration had to be done, false = config file is up-to-date
            @Override
            public boolean checkAndMigrate(FileConfiguration configuration, PropertyMap propertyMap, File pluginFolder) {
                return !super.containsAllSettings(configuration, propertyMap);
            }
        };
    }

}
