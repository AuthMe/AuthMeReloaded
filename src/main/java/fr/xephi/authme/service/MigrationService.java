package fr.xephi.authme.service;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.converter.ForceFlatToSqlite;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.datasource.FlatFile;
import fr.xephi.authme.datasource.SQLite;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.security.crypts.SHA256;
import fr.xephi.authme.settings.Settings;
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
    public static void changePlainTextToSha256(Settings settings, DataSource dataSource,
                                               SHA256 authmeSha256) {
        if (HashAlgorithm.PLAINTEXT == settings.getProperty(SecuritySettings.PASSWORD_HASH)) {
            ConsoleLogger.warning("Your HashAlgorithm has been detected as plaintext and is now deprecated;"
                + " it will be changed and hashed now to the AuthMe default hashing method");
            ConsoleLogger.warning("Don't stop your server; wait for the conversion to have been completed!");
            List<PlayerAuth> allAuths = dataSource.getAllAuths();
            for (PlayerAuth auth : allAuths) {
                String hash = auth.getPassword().getHash();
                if (hash.startsWith("$SHA$")) {
                    ConsoleLogger.warning("Skipping conversion for " + auth.getNickname() + "; detected SHA hash");
                } else {
                    HashedPassword hashedPassword = authmeSha256.computeHash(hash, auth.getNickname());
                    auth.setPassword(hashedPassword);
                    dataSource.updatePassword(auth);
                }
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
    public static DataSource convertFlatfileToSqlite(Settings settings, DataSource dataSource) {
        if (DataSourceType.FILE == settings.getProperty(DatabaseSettings.BACKEND)) {
            ConsoleLogger.warning("FlatFile backend has been detected and is now deprecated; it will be changed "
                + "to SQLite... Connection will be impossible until conversion is done!");
            FlatFile flatFile = (FlatFile) dataSource;
            try {
                SQLite sqlite = new SQLite(settings);
                ForceFlatToSqlite converter = new ForceFlatToSqlite(flatFile, sqlite);
                converter.execute(null);
                settings.setProperty(DatabaseSettings.BACKEND, DataSourceType.SQLITE);
                settings.save();
                return sqlite;
            } catch (Exception e) {
                ConsoleLogger.logException("Error during conversion from Flatfile to SQLite", e);
                throw new IllegalStateException(e);
            }
        }
        return null;
    }

}
