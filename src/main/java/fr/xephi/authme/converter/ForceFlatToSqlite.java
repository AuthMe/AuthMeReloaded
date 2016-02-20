package fr.xephi.authme.converter;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.datasource.SQLite;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import java.sql.SQLException;

/**
 * Mandatory migration from the deprecated flat file datasource to SQLite.
 */
public class ForceFlatToSqlite {

    private final DataSource database;
    private final NewSetting settings;

    public ForceFlatToSqlite(DataSource database, NewSetting settings) {
        this.database = database;
        this.settings = settings;
    }

    public DataSource run() {
        try {
            DataSource sqlite = new SQLite(settings);
            for (PlayerAuth auth : database.getAllAuths()) {
                auth.setRealName("Player");
                sqlite.saveAuth(auth);
            }
            settings.setProperty(DatabaseSettings.BACKEND, DataSourceType.SQLITE);
            settings.save();
            ConsoleLogger.info("Database successfully converted to sqlite!");
            return sqlite;
        } catch (SQLException | ClassNotFoundException e) {
            ConsoleLogger.logException("Could not convert from Flatfile to SQLite:", e);
        }
        return null;
    }
}
