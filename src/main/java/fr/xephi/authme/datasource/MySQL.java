package fr.xephi.authme.datasource;

import com.google.common.annotations.VisibleForTesting;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool.PoolInitializationException;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.sqlextensions.SqlExtensionsFactory;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.HooksSettings;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import static fr.xephi.authme.datasource.SqlDataSourceUtils.isColumnMissing;

public class MySQL extends SqlDataSource {

    public MySQL(Settings settings, SqlExtensionsFactory extensionsFactory) throws SQLException {
        setParameters(settings, extensionsFactory);

        // Set the connection arguments (and check if connection is ok)
        try {
            this.setConnectionArguments();
        } catch (RuntimeException e) {
            if (e instanceof IllegalArgumentException) {
                ConsoleLogger.warning("Invalid database arguments! Please check your configuration!");
                ConsoleLogger.warning("If this error persists, please report it to the developer!");
            }
            if (e instanceof PoolInitializationException) {
                ConsoleLogger.warning("Can't initialize database connection! Please check your configuration!");
                ConsoleLogger.warning("If this error persists, please report it to the developer!");
            }
            ConsoleLogger.warning("Can't use the Hikari Connection Pool! Please, report this error to the developer!");
            throw e;
        }

        // Initialize the database
        try {
            checkTablesAndColumns();
        } catch (SQLException e) {
            closeConnection();
            ConsoleLogger.logException("Can't initialize the database:", e);
            ConsoleLogger.warning("Please check your database settings in the config.yml file!");
            throw e;
        }
    }

    @VisibleForTesting
    MySQL(Settings settings, HikariDataSource hikariDataSource, SqlExtensionsFactory extensionsFactory) {
        ds = hikariDataSource;
        setParameters(settings, extensionsFactory);
    }

    /**
     * Retrieves various settings.
     *
     * @param settings the settings to read properties from
     * @param extensionsFactory factory to create the MySQL extension
     */
    private void setParameters(Settings settings, SqlExtensionsFactory extensionsFactory) {
        this.host = settings.getProperty(DatabaseSettings.MYSQL_HOST);
        this.port = settings.getProperty(DatabaseSettings.MYSQL_PORT);
        this.username = settings.getProperty(DatabaseSettings.MYSQL_USERNAME);
        this.password = settings.getProperty(DatabaseSettings.MYSQL_PASSWORD);
        this.database = settings.getProperty(DatabaseSettings.MYSQL_DATABASE);
        this.tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);
        this.columnOthers = settings.getProperty(HooksSettings.MYSQL_OTHER_USERNAME_COLS);
        this.col = new Columns(settings);
        this.sqlExtension = extensionsFactory.buildExtension(col);
        this.poolSize = settings.getProperty(DatabaseSettings.MYSQL_POOL_SIZE);
        this.maxLifetime = settings.getProperty(DatabaseSettings.MYSQL_CONNECTION_MAX_LIFETIME);
        this.useSsl = settings.getProperty(DatabaseSettings.MYSQL_USE_SSL);
    }

    void  initHikariDataSource() {

        ds = new HikariDataSource();
        ds.setPoolName("AuthMeMYSQLPool");

        // Database URL
        ds.setJdbcUrl("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database);
    }

    /**
     * Sets up the connection arguments to the database.
     */
    void setConnectionArguments() {

        initHikariDataSource();

        // Pool Settings
        ds.setMaximumPoolSize(poolSize);
        ds.setMaxLifetime(maxLifetime * 1000);

        // Auth
        ds.setUsername(this.username);
        ds.setPassword(this.password);

        // Request mysql over SSL
        ds.addDataSourceProperty("useSSL", String.valueOf(useSsl));

        // Encoding
        ds.addDataSourceProperty("characterEncoding", "utf8");
        ds.addDataSourceProperty("encoding", "UTF-8");
        ds.addDataSourceProperty("useUnicode", "true");

        // Random stuff
        ds.addDataSourceProperty("rewriteBatchedStatements", "true");
        ds.addDataSourceProperty("jdbcCompliantTruncation", "false");

        // Caching
        ds.addDataSourceProperty("cachePrepStmts", "true");
        ds.addDataSourceProperty("prepStmtCacheSize", "275");
        ds.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        ConsoleLogger.info("Connection arguments loaded, Hikari ConnectionPool ready!");
    }

    /**
     * Creates the table or any of its required columns if they don't exist.
     */
    private void checkTablesAndColumns() throws SQLException {
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            // Create table with ID column if it doesn't exist
            String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + col.ID + " MEDIUMINT(8) UNSIGNED AUTO_INCREMENT,"
                + "PRIMARY KEY (" + col.ID + ")"
                + ") CHARACTER SET = utf8;";
            st.executeUpdate(sql);

            DatabaseMetaData md = con.getMetaData();
            if (isColumnMissing(md, col.NAME, tableName)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.NAME + " VARCHAR(255) NOT NULL UNIQUE AFTER " + col.ID + ";");
            }

            if (isColumnMissing(md, col.REAL_NAME, tableName)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.REAL_NAME + " VARCHAR(255) NOT NULL AFTER " + col.NAME + ";");
            }

            if (isColumnMissing(md, col.PASSWORD, tableName)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.PASSWORD + " VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL;");
            }

            if (!col.SALT.isEmpty() && isColumnMissing(md, col.SALT, tableName)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + col.SALT + " VARCHAR(255);");
            }

            if (isColumnMissing(md, col.LAST_IP, tableName)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.LAST_IP + " VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin;");
            } else {
                MySqlMigrater.migrateLastIpColumn(st, md, tableName, col);
            }

            if (isColumnMissing(md, col.LAST_LOGIN, tableName)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.LAST_LOGIN + " BIGINT;");
            } else {
                MySqlMigrater.migrateLastLoginColumn(st, md, tableName, col);
            }

            if (isColumnMissing(md, col.REGISTRATION_DATE, tableName)) {
                MySqlMigrater.addRegistrationDateColumn(st, tableName, col);
            }

            if (isColumnMissing(md, col.REGISTRATION_IP, tableName)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.REGISTRATION_IP + " VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin;");
            }

            if (isColumnMissing(md, col.LASTLOC_X, tableName)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.LASTLOC_X + " DOUBLE NOT NULL DEFAULT '0.0' AFTER " + col.LAST_LOGIN + " , ADD "
                    + col.LASTLOC_Y + " DOUBLE NOT NULL DEFAULT '0.0' AFTER " + col.LASTLOC_X + " , ADD "
                    + col.LASTLOC_Z + " DOUBLE NOT NULL DEFAULT '0.0' AFTER " + col.LASTLOC_Y);
            } else {
                st.executeUpdate("ALTER TABLE " + tableName + " MODIFY "
                    + col.LASTLOC_X + " DOUBLE NOT NULL DEFAULT '0.0', MODIFY "
                    + col.LASTLOC_Y + " DOUBLE NOT NULL DEFAULT '0.0', MODIFY "
                    + col.LASTLOC_Z + " DOUBLE NOT NULL DEFAULT '0.0';");
            }

            if (isColumnMissing(md, col.LASTLOC_WORLD, tableName)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.LASTLOC_WORLD + " VARCHAR(255) NOT NULL DEFAULT 'world' AFTER " + col.LASTLOC_Z);
            }

            if (isColumnMissing(md, col.LASTLOC_YAW, tableName)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.LASTLOC_YAW + " FLOAT;");
            }

            if (isColumnMissing(md, col.LASTLOC_PITCH, tableName)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.LASTLOC_PITCH + " FLOAT;");
            }

            if (isColumnMissing(md, col.EMAIL, tableName)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.EMAIL + " VARCHAR(255);");
            }

            if (isColumnMissing(md, col.IS_LOGGED, tableName)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.IS_LOGGED + " SMALLINT NOT NULL DEFAULT '0' AFTER " + col.EMAIL);
            }

            if (isColumnMissing(md, col.HAS_SESSION, tableName)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.HAS_SESSION + " SMALLINT NOT NULL DEFAULT '0' AFTER " + col.IS_LOGGED);
            }
        }
        ConsoleLogger.info("MySQL setup finished");
    }

    @Override
    public DataSourceType getType() { return DataSourceType.MYSQL; }
}
