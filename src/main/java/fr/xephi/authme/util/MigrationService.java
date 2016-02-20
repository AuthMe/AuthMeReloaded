package fr.xephi.authme.util;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.converter.ForceFlatToSqlite;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.security.crypts.SHA256;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;

import java.util.List;

/**
 * Migrations to perform during the initialization of AuthMe.
 */
public final class MigrationService {

    private MigrationService() {
    }

    /**
     * Hash all passwords to SHA256 and updated the setting if the password hash is set to the deprecated PLAINTEXT.
     *
     * @param settings The settings instance
     * @param dataSource The data source
     * @param authmeSha256 Instance to the AuthMe SHA256 encryption method implementation
     */
    public static void changePlainTextToSha256(NewSetting settings, DataSource dataSource,
                                               SHA256 authmeSha256) {
        if (HashAlgorithm.PLAINTEXT == settings.getProperty(SecuritySettings.PASSWORD_HASH)) {
            ConsoleLogger.showError("Your HashAlgorithm has been detected as plaintext and is now deprecated;"
                + " it will be changed and hashed now to the AuthMe default hashing method");
            List<PlayerAuth> allAuths = dataSource.getAllAuths();
            for (PlayerAuth auth : allAuths) {
                HashedPassword hashedPassword = authmeSha256.computeHash(
                    auth.getPassword().getHash(), auth.getNickname());
                auth.setPassword(hashedPassword);
                dataSource.updatePassword(auth);
            }
            settings.setProperty(SecuritySettings.PASSWORD_HASH, HashAlgorithm.SHA256);
            settings.save();
            ConsoleLogger.info("Migrated " + allAuths.size() + " accounts from plaintext to SHA256");
        }
    }

    /**
     * Converts the data source from the deprecated FLATFILE type to SQLITE.
     *
     * @param settings The settings instance
     * @param dataSource The data source
     * @return The converted datasource (SQLite), or null if no migration was necessary
     */
    public static DataSource convertFlatfileToSqlite(NewSetting settings, DataSource dataSource) {
        if (DataSourceType.FILE == settings.getProperty(DatabaseSettings.BACKEND)) {
            ConsoleLogger.showError("FlatFile backend has been detected and is now deprecated; it will be changed "
                + "to SQLite... Connection will be impossible until conversion is done!");
            ForceFlatToSqlite converter = new ForceFlatToSqlite(dataSource, settings);
            DataSource result = converter.run();
            if (result == null) {
                throw new IllegalStateException("Error during conversion from flatfile to SQLite");
            } else {
                return result;
            }
        }
        return null;
    }

}
