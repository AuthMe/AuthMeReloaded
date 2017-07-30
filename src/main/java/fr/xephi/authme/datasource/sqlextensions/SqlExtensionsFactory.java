package fr.xephi.authme.datasource.sqlextensions;

import fr.xephi.authme.datasource.Columns;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;

import javax.inject.Inject;

/**
 * Creates the appropriate {@link SqlExtension}, depending on the configured password hashing algorithm.
 */
public class SqlExtensionsFactory {

    @Inject
    private Settings settings;

    /**
     * Creates a new {@link SqlExtension} object according to the configured hash algorithm.
     *
     * @param columnsConfig the columns configuration
     * @return the extension the MySQL data source should use
     */
    public SqlExtension buildExtension(Columns columnsConfig) {
        HashAlgorithm hash = settings.getProperty(SecuritySettings.PASSWORD_HASH);
        switch (hash) {
            case IPB4:
                return new Ipb4Extension(settings, columnsConfig);
            case PHPBB:
                return new PhpBbExtension(settings, columnsConfig);
            case WORDPRESS:
                return new WordpressExtension(settings, columnsConfig);
            case XFBCRYPT:
                return new XfBcryptExtension(settings, columnsConfig);
            default:
                return new NoOpExtension(settings, columnsConfig);
        }
    }
}
