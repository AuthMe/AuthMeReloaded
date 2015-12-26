package fr.xephi.authme.converter;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.SQLite;
import fr.xephi.authme.settings.Settings;

/**
 */
public class ForceFlatToSqlite {

    private final DataSource data;

    /**
     * Constructor for ForceFlatToSqlite.
     *
     * @param data   DataSource
     * @param plugin AuthMe
     */
    public ForceFlatToSqlite(DataSource data, AuthMe plugin) {
        this.data = data;
    }

    /**
     * Method run.
     *
     * @see java.lang.Runnable#run()
     */
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
            ConsoleLogger.showError("An error occured while trying to convert flatfile to sqlite ...");
            return null;
        }
        return sqlite;
    }
}
