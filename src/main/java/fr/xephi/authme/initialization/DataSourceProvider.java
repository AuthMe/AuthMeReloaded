package fr.xephi.authme.initialization;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.CacheDataSource;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.datasource.MySQL;
import fr.xephi.authme.datasource.PostgreSqlDataSource;
import fr.xephi.authme.datasource.SQLite;
import fr.xephi.authme.datasource.mysqlextensions.MySqlExtensionsFactory;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Creates the AuthMe data source.
 */
public class DataSourceProvider implements Provider<DataSource> {

    private static final int SQLITE_MAX_SIZE = 4000;

    @Inject
    @DataFolder
    private File dataFolder;
    @Inject
    private Settings settings;
    @Inject
    private BukkitService bukkitService;
    @Inject
    private PlayerCache playerCache;
    @Inject
    private MySqlExtensionsFactory mySqlExtensionsFactory;

    DataSourceProvider() {
    }

    @Override
    public DataSource get() {
        try {
            return createDataSource();
        } catch (Exception e) {
            ConsoleLogger.logException("Could not create data source:", e);
            throw new IllegalStateException("Error during initialization of data source", e);
        }
    }

    /**
     * Sets up the data source.
     *
     * @return the constructed datasource
     * @throws SQLException           when initialization of a SQL datasource failed
     * @throws IOException            if flat file cannot be read
     */
    private DataSource createDataSource() throws SQLException, IOException {
        DataSourceType dataSourceType = settings.getProperty(DatabaseSettings.BACKEND);
        DataSource dataSource;
        switch (dataSourceType) {
            case MYSQL:
                dataSource = new MySQL(settings, mySqlExtensionsFactory);
                break;
            case POSTGRESQL:
                dataSource = new PostgreSqlDataSource(settings, mySqlExtensionsFactory);
                break;
            case SQLITE:
                dataSource = new SQLite(settings, dataFolder);
                break;
            default:
                throw new UnsupportedOperationException("Unknown data source type '" + dataSourceType + "'");
        }

        if (settings.getProperty(DatabaseSettings.USE_CACHING)) {
            dataSource = new CacheDataSource(dataSource, playerCache);
        }
        if (DataSourceType.SQLITE.equals(dataSourceType)) {
            checkDataSourceSize(dataSource);
        }
        return dataSource;
    }

    private void checkDataSourceSize(DataSource dataSource) {
        bukkitService.runTaskAsynchronously(() -> {
            int accounts = dataSource.getAccountsRegistered();
            if (accounts >= SQLITE_MAX_SIZE) {
                ConsoleLogger.warning("YOU'RE USING THE SQLITE DATABASE WITH "
                    + accounts + "+ ACCOUNTS; FOR BETTER PERFORMANCE, PLEASE UPGRADE TO MYSQL!!");
            }
        });
    }
}
