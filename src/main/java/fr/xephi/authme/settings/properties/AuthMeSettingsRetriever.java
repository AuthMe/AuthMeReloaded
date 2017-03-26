package fr.xephi.authme.settings.properties;

import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.configurationdata.ConfigurationDataBuilder;
import ch.jalu.configme.properties.Property;

/**
 * Utility class responsible for retrieving all {@link Property} fields from {@link SettingsHolder} classes.
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
            DatabaseSettings.class,  PluginSettings.class,    RestrictionSettings.class,
            EmailSettings.class,     HooksSettings.class,     ProtectionSettings.class,
            PurgeSettings.class,     SecuritySettings.class,  RegistrationSettings.class,
            LimboSettings.class,     BackupSettings.class,    ConverterSettings.class);
    }
}
