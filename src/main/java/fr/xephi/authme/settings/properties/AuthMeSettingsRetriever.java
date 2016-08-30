package fr.xephi.authme.settings.properties;

import com.github.authme.configme.SettingsHolder;
import com.github.authme.configme.properties.Property;
import com.github.authme.configme.propertymap.PropertyEntry;
import com.github.authme.configme.propertymap.SettingsFieldRetriever;

import java.util.List;

/**
 * Utility class responsible for retrieving all {@link Property} fields
 * from {@link SettingsHolder} implementations via reflection.
 */
public final class AuthMeSettingsRetriever {

    private AuthMeSettingsRetriever() {
    }

    /**
     * Constructs a list with all property fields in AuthMe {@link SettingsHolder} classes.
     *
     * @return list of all known properties
     */
    public static List<PropertyEntry> getAllPropertyFields() {
        SettingsFieldRetriever retriever = new SettingsFieldRetriever(
            DatabaseSettings.class,      ConverterSettings.class,  PluginSettings.class,
            RestrictionSettings.class,   EmailSettings.class,      HooksSettings.class,
            ProtectionSettings.class,    PurgeSettings.class,      SecuritySettings.class,
            RegistrationSettings.class,  BackupSettings.class);

        return retriever.getAllPropertyFields();
    }
}
