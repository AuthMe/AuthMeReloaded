package fr.xephi.authme.settings.properties;

import com.github.authme.configme.SettingsHolder;
import com.github.authme.configme.knownproperties.ConfigurationData;
import com.github.authme.configme.knownproperties.ConfigurationDataBuilder;
import com.github.authme.configme.properties.Property;

/**
 * Utility class responsible for retrieving all {@link Property} fields
 * from {@link SettingsHolder} implementations via reflection.
 */
public final class AuthMeSettingsRetriever {

    private AuthMeSettingsRetriever() {
    }

    /**
     * Builds the configuration data for all property fields in AuthMe {@link SettingsHolder} classes.
     *
     * @return configuration data
     */
    public static ConfigurationData buildConfigurationData() {
        return ConfigurationDataBuilder.collectData(
            DatabaseSettings.class,      ConverterSettings.class,  PluginSettings.class,
            RestrictionSettings.class,   EmailSettings.class,      HooksSettings.class,
            ProtectionSettings.class,    PurgeSettings.class,      SecuritySettings.class,
            RegistrationSettings.class,  BackupSettings.class);
    }
}
