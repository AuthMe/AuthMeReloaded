package fr.xephi.authme.converter;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.SQLite;
import fr.xephi.authme.settings.Settings;

/**
 */
public class ForceFlatToSqlite {

    private final DataSource data;

    public ForceFlatToSqlite(DataSource data) {
        this.data = data;
    }

    public DataSource run() {
        DataSource sqlite = null;
        try {
            sqlite = new SQLite();
            for (PlayerAuth auth : data.getAllAuths()) {
                auth.setRealName("Player");
                sqlite.saveAuth(auth);
            }
            Settings.setValue("DataSource.backend", "sqlite");
            ConsoleLogger.info("Database successfully converted to sqlite !");
        } catch (Exception e) {
            ConsoleLogger.showError("An error occurred while trying to convert flatfile to sqlite ...");
            return null;
        }
        return sqlite;
    }
}
