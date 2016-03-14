package fr.xephi.authme.settings;

import fr.xephi.authme.settings.propertymap.PropertyMap;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

/**
 * Simple settings migration service simply returning {@code true} if all properties are present,
 * {@code false} otherwise.
 */
public class PlainSettingsMigrationService extends SettingsMigrationService {

    // See parent javadoc: true = some migration had to be done, false = config file is up-to-date
    @Override
    public boolean checkAndMigrate(FileConfiguration configuration, PropertyMap propertyMap, File pluginFolder) {
        return !super.containsAllSettings(configuration, propertyMap);
    }

}
