package fr.xephi.authme.initialization;

import com.github.authme.configme.knownproperties.ConfigurationData;
import com.github.authme.configme.resource.PropertyResource;
import com.github.authme.configme.resource.YamlFileResource;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SettingsMigrationService;
import fr.xephi.authme.settings.properties.AuthMeSettingsRetriever;
import fr.xephi.authme.util.FileUtils;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;

/**
 * Initializes the settings.
 */
public class SettingsProvider implements Provider<Settings> {

    @Inject
    @DataFolder
    private File dataFolder;
    @Inject
    private SettingsMigrationService migrationService;

    SettingsProvider() {
    }

    /**
     * Loads the plugin's settings.
     *
     * @return the settings instance, or null if it could not be constructed
     */
    @Override
    public Settings get() {
        File configFile = new File(dataFolder, "config.yml");
        if (!configFile.exists()) {
            FileUtils.create(configFile);
        }
        PropertyResource resource = new YamlFileResource(configFile);
        ConfigurationData configurationData = AuthMeSettingsRetriever.buildConfigurationData();
        return new Settings(dataFolder, resource, migrationService, configurationData);
    }

}
