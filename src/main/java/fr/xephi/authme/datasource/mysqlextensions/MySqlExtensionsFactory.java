package fr.xephi.authme.datasource.mysqlextensions;

import fr.xephi.authme.datasource.Columns;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;

import javax.inject.Inject;

/**
 * Creates the appropriate {@link MySqlExtension}, depending on the configured password hashing algorithm.
 */
public class MySqlExtensionsFactory {

    @Inject
    private Settings settings;

    /**
     * Creates a new {@link MySqlExtension} object according to the configured hash algorithm.
     *
     * @param columnsConfig the columns configuration
     * @return the extension the MySQL data source should use
     */
    public MySqlExtension buildExtension(Columns columnsConfig) {
        HashAlgorithm hash = settings.getProperty(SecuritySettings.PASSWORD_HASH);
        return switch (hash) {
            case IPB4 -> new Ipb4Extension(settings, columnsConfig);
            case PHPBB -> new PhpBbExtension(settings, columnsConfig);
            case WORDPRESS -> new WordpressExtension(settings, columnsConfig);
            case XFBCRYPT -> new XfBcryptExtension(settings, columnsConfig);
            default -> new NoOpExtension(settings, columnsConfig);
        };
    }
}
