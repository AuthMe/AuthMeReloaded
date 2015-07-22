package fr.xephi.authme.converter;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.SQLite;

public class ForceFlatToSqlite implements Converter {

    private DataSource data;
    private AuthMe plugin;

    public ForceFlatToSqlite(DataSource data, AuthMe plugin) {
        this.data = data;
        this.plugin = plugin;
    }

    @Override
    public void run() {
        DataSource sqlite = null;
        try {
            sqlite = new SQLite();
            for (PlayerAuth auth : data.getAllAuths())
                sqlite.saveAuth(auth);
            plugin.getSettings().setValue("DataSource.backend", "sqlite");
            ConsoleLogger.info("Database successfully converted to sqlite !");
        } catch (Exception e) {
            ConsoleLogger.showError("An error appeared while trying to convert flatfile to sqlite ...");
        } finally {
            if (sqlite != null)
                sqlite.close();
        }
    }
}
