package fr.xephi.authme.datasource;

import com.google.common.annotations.VisibleForTesting;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool.PoolInitializationException;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.columnshandler.AuthMeColumnsHandler;
import fr.xephi.authme.datasource.mysqlextensions.MySqlExtension;
import fr.xephi.authme.datasource.mysqlextensions.MySqlExtensionsFactory;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.HooksSettings;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fr.xephi.authme.datasource.SqlDataSourceUtils.getNullableLong;
import static fr.xephi.authme.datasource.SqlDataSourceUtils.logSqlException;

/**
 * PostgreSQL data source.
 */
public class PostgreSqlDataSource extends AbstractSqlDataSource {

    private String host;
    private String port;
    private String username;
    private String password;
    private String database;
    private String tableName;
    private int poolSize;
    private int maxLifetime;
    private List<String> columnOthers;
    private Columns col;
    private MySqlExtension sqlExtension;
    private HikariDataSource ds;

    public PostgreSqlDataSource(Settings settings, MySqlExtensionsFactory extensionsFactory) throws SQLException {
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
            ConsoleLogger.logException("Can't initialize the PostgreSQL database:", e);
            ConsoleLogger.warning("Please check your database settings in the config.yml file!");
            throw e;
        }
    }

    @VisibleForTesting
    PostgreSqlDataSource(Settings settings, HikariDataSource hikariDataSource,
                         MySqlExtensionsFactory extensionsFactory) {
        ds = hikariDataSource;
        setParameters(settings, extensionsFactory);
    }

    /**
     * Retrieves various settings.
     *
     * @param settings          the settings to read properties from
     * @param extensionsFactory factory to create the MySQL extension
     */
    private void setParameters(Settings settings, MySqlExtensionsFactory extensionsFactory) {
        this.host = settings.getProperty(DatabaseSettings.MYSQL_HOST);
        this.port = settings.getProperty(DatabaseSettings.MYSQL_PORT);
        this.username = settings.getProperty(DatabaseSettings.MYSQL_USERNAME);
        this.password = settings.getProperty(DatabaseSettings.MYSQL_PASSWORD);
        this.database = settings.getProperty(DatabaseSettings.MYSQL_DATABASE);
        this.tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);
        this.columnOthers = settings.getProperty(HooksSettings.MYSQL_OTHER_USERNAME_COLS);
        this.col = new Columns(settings);
        this.columnsHandler = AuthMeColumnsHandler.createForMySql(this::getConnection, settings);
        this.sqlExtension = extensionsFactory.buildExtension(col);
        this.poolSize = settings.getProperty(DatabaseSettings.MYSQL_POOL_SIZE);
        this.maxLifetime = settings.getProperty(DatabaseSettings.MYSQL_CONNECTION_MAX_LIFETIME);
    }

    /**
     * Sets up the connection arguments to the database.
     */
    private void setConnectionArguments() {
        ds = new HikariDataSource();
        ds.setPoolName("AuthMePostgreSQLPool");

        // Pool Settings
        ds.setMaximumPoolSize(poolSize);
        ds.setMaxLifetime(maxLifetime * 1000);

        // Database URL
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setJdbcUrl("jdbc:postgresql://" + this.host + ":" + this.port + "/" + this.database);

        // Auth
        ds.setUsername(this.username);
        ds.setPassword(this.password);

        // Random stuff
        ds.addDataSourceProperty("reWriteBatchedInserts", "true");

        // Caching
        ds.addDataSourceProperty("cachePrepStmts", "true");
        ds.addDataSourceProperty("preparedStatementCacheQueries", "275");

        ConsoleLogger.info("Connection arguments loaded, Hikari ConnectionPool ready!");
    }

    @Override
    public void reload() {
        if (ds != null) {
            ds.close();
        }
        setConnectionArguments();
        ConsoleLogger.info("Hikari ConnectionPool arguments reloaded!");
    }

    private Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    /**
     * Creates the table or any of its required columns if they don't exist.
     */
    private void checkTablesAndColumns() throws SQLException {
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            // Create table with ID column if it doesn't exist
            String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + col.ID + " BIGSERIAL,"
                + "PRIMARY KEY (" + col.ID + ")"
                + ");";
            st.executeUpdate(sql);

            DatabaseMetaData md = con.getMetaData();
            if (isColumnMissing(md, col.NAME)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.NAME + " VARCHAR(255) NOT NULL UNIQUE;");
            }

            if (isColumnMissing(md, col.REAL_NAME)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.REAL_NAME + " VARCHAR(255) NOT NULL;");
            }

            if (isColumnMissing(md, col.PASSWORD)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.PASSWORD + " VARCHAR(255) NOT NULL;");
            }

            if (!col.SALT.isEmpty() && isColumnMissing(md, col.SALT)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + col.SALT + " VARCHAR(255);");
            }

            if (isColumnMissing(md, col.LAST_IP)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.LAST_IP + " VARCHAR(40);");
            } else {
                MySqlMigrater.migrateLastIpColumn(st, md, tableName, col);
            }

            if (isColumnMissing(md, col.LAST_LOGIN)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.LAST_LOGIN + " BIGINT;");
            } else {
                MySqlMigrater.migrateLastLoginColumn(st, md, tableName, col);
            }

            if (isColumnMissing(md, col.REGISTRATION_DATE)) {
                MySqlMigrater.addRegistrationDateColumn(st, tableName, col);
            }

            if (isColumnMissing(md, col.REGISTRATION_IP)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.REGISTRATION_IP + " VARCHAR(40);");
            }

            if (isColumnMissing(md, col.LASTLOC_X)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.LASTLOC_X + " DOUBLE PRECISION NOT NULL DEFAULT '0.0' , ADD "
                    + col.LASTLOC_Y + " DOUBLE PRECISION NOT NULL DEFAULT '0.0' , ADD "
                    + col.LASTLOC_Z + " DOUBLE PRECISION NOT NULL DEFAULT '0.0';");
            }

            if (isColumnMissing(md, col.LASTLOC_WORLD)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.LASTLOC_WORLD + " VARCHAR(255) NOT NULL DEFAULT 'world';");
            }

            if (isColumnMissing(md, col.LASTLOC_YAW)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.LASTLOC_YAW + " FLOAT;");
            }

            if (isColumnMissing(md, col.LASTLOC_PITCH)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.LASTLOC_PITCH + " FLOAT;");
            }

            if (isColumnMissing(md, col.EMAIL)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.EMAIL + " VARCHAR(255);");
            }

            if (isColumnMissing(md, col.IS_LOGGED)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.IS_LOGGED + " SMALLINT NOT NULL DEFAULT '0';");
            }

            if (isColumnMissing(md, col.HAS_SESSION)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.HAS_SESSION + " SMALLINT NOT NULL DEFAULT '0';");
            }

            if (isColumnMissing(md, col.TOTP_KEY)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.TOTP_KEY + " VARCHAR(16);");
            }

            if (!col.PLAYER_UUID.isEmpty() && isColumnMissing(md, col.PLAYER_UUID)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.PLAYER_UUID + " VARCHAR(36)");
            }
        }
        ConsoleLogger.info("PostgreSQL setup finished");
    }

    private boolean isColumnMissing(DatabaseMetaData metaData, String columnName) throws SQLException {
        try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName.toLowerCase())) {
            return !rs.next();
        }
    }

    @Override
    public PlayerAuth getAuth(String user) {
        String sql = "SELECT * FROM " + tableName + " WHERE " + col.NAME + "=?;";
        PlayerAuth auth;
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user.toLowerCase());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(col.ID);
                    auth = buildAuthFromResultSet(rs);
                    sqlExtension.extendAuth(auth, id, con);
                    return auth;
                }
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return null;
    }

    @Override
    public boolean saveAuth(PlayerAuth auth) {
        super.saveAuth(auth);

        try (Connection con = getConnection()) {
            if (!columnOthers.isEmpty()) {
                for (String column : columnOthers) {
                    try (PreparedStatement pst = con.prepareStatement(
                        "UPDATE " + tableName + " SET " + column + "=? WHERE " + col.NAME + "=?;")) {
                        pst.setString(1, auth.getRealName());
                        pst.setString(2, auth.getNickname());
                        pst.executeUpdate();
                    }
                }
            }

            sqlExtension.saveAuth(auth, con);
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public Set<String> getRecordsToPurge(long until) {
        Set<String> list = new HashSet<>();
        String select = "SELECT " + col.NAME + " FROM " + tableName + " WHERE GREATEST("
            + " COALESCE(" + col.LAST_LOGIN + ", 0),"
            + " COALESCE(" + col.REGISTRATION_DATE + ", 0)"
            + ") < ?;";
        try (Connection con = getConnection();
             PreparedStatement selectPst = con.prepareStatement(select)) {
            selectPst.setLong(1, until);
            try (ResultSet rs = selectPst.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString(col.NAME));
                }
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }

        return list;
    }

    @Override
    public boolean removeAuth(String user) {
        user = user.toLowerCase();
        String sql = "DELETE FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            sqlExtension.removeAuth(user, con);
            pst.setString(1, user.toLowerCase());
            pst.executeUpdate();
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public void closeConnection() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
    }

    @Override
    public void purgeRecords(Collection<String> toPurge) {
        String sql = "DELETE FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            for (String name : toPurge) {
                pst.setString(1, name.toLowerCase());
                pst.executeUpdate();
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.POSTGRESQL;
    }

    @Override
    public List<PlayerAuth> getAllAuths() {
        List<PlayerAuth> auths = new ArrayList<>();
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT * FROM " + tableName)) {
                while (rs.next()) {
                    PlayerAuth auth = buildAuthFromResultSet(rs);
                    sqlExtension.extendAuth(auth, rs.getInt(col.ID), con);
                    auths.add(auth);
                }
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return auths;
    }

    @Override
    public List<String> getLoggedPlayersWithEmptyMail() {
        List<String> players = new ArrayList<>();
        String sql = "SELECT " + col.REAL_NAME + " FROM " + tableName + " WHERE " + col.IS_LOGGED + " = 1"
            + " AND (" + col.EMAIL + " = 'your@email.com' OR " + col.EMAIL + " IS NULL);";
        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                players.add(rs.getString(1));
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return players;
    }

    @Override
    public List<PlayerAuth> getRecentlyLoggedInPlayers() {
        List<PlayerAuth> players = new ArrayList<>();
        String sql = "SELECT * FROM " + tableName + " ORDER BY " + col.LAST_LOGIN + " DESC LIMIT 10;";
        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                players.add(buildAuthFromResultSet(rs));
            }
        } catch (SQLException e) {
            logSqlException(e);
        }
        return players;
    }

    @Override
    public boolean setTotpKey(String user, String totpKey) {
        String sql = "UPDATE " + tableName + " SET " + col.TOTP_KEY + " = ? WHERE " + col.NAME + " = ?";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, totpKey);
            pst.setString(2, user.toLowerCase());
            pst.executeUpdate();
            return true;
        } catch (SQLException e) {
            logSqlException(e);
        }
        return false;
    }

    /**
     * Creates a {@link PlayerAuth} object with the data from the provided result set.
     *
     * @param row the result set to read from
     *
     * @return generated player auth object with the data from the result set
     *
     * @throws SQLException .
     */
    private PlayerAuth buildAuthFromResultSet(ResultSet row) throws SQLException {
        String salt = col.SALT.isEmpty() ? null : row.getString(col.SALT);
        int group = col.GROUP.isEmpty() ? -1 : row.getInt(col.GROUP);
        return PlayerAuth.builder()
            .name(row.getString(col.NAME))
            .realName(row.getString(col.REAL_NAME))
            .password(row.getString(col.PASSWORD), salt)
            .totpKey(row.getString(col.TOTP_KEY))
            .lastLogin(getNullableLong(row, col.LAST_LOGIN))
            .lastIp(row.getString(col.LAST_IP))
            .email(row.getString(col.EMAIL))
            .registrationDate(row.getLong(col.REGISTRATION_DATE))
            .registrationIp(row.getString(col.REGISTRATION_IP))
            .groupId(group)
            .locWorld(row.getString(col.LASTLOC_WORLD))
            .locX(row.getDouble(col.LASTLOC_X))
            .locY(row.getDouble(col.LASTLOC_Y))
            .locZ(row.getDouble(col.LASTLOC_Z))
            .locYaw(row.getFloat(col.LASTLOC_YAW))
            .locPitch(row.getFloat(col.LASTLOC_PITCH))
            .build();
    }
}
