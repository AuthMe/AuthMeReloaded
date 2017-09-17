package fr.xephi.authme.datasource;

import com.google.common.annotations.VisibleForTesting;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool.PoolInitializationException;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.mysqlextensions.MySqlExtension;
import fr.xephi.authme.datasource.mysqlextensions.MySqlExtensionsFactory;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.util.StringUtils;
import fr.xephi.authme.util.Utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fr.xephi.authme.datasource.SqlDataSourceUtils.logSqlException;

public class MySQL implements DataSource {

    private boolean useSsl;
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

    public MySQL(Settings settings, MySqlExtensionsFactory extensionsFactory) throws SQLException {
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
            ConsoleLogger.logException("Can't initialize the MySQL database:", e);
            ConsoleLogger.warning("Please check your database settings in the config.yml file!");
            throw e;
        }
    }

    @VisibleForTesting
    MySQL(Settings settings, HikariDataSource hikariDataSource, MySqlExtensionsFactory extensionsFactory) {
        ds = hikariDataSource;
        setParameters(settings, extensionsFactory);
    }

    /**
     * Retrieves various settings.
     *
     * @param settings the settings to read properties from
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
        sqlExtension = extensionsFactory.buildExtension(col);
        this.poolSize = settings.getProperty(DatabaseSettings.MYSQL_POOL_SIZE);
        if (poolSize == -1) {
            poolSize = Utils.getCoreCount() * 3;
        }
        this.maxLifetime = settings.getProperty(DatabaseSettings.MYSQL_CONNECTION_MAX_LIFETIME);
        this.useSsl = settings.getProperty(DatabaseSettings.MYSQL_USE_SSL);
    }

    /**
     * Sets up the connection arguments to the database.
     */
    private void setConnectionArguments() {
        ds = new HikariDataSource();
        ds.setPoolName("AuthMeMYSQLPool");

        // Pool Settings
        ds.setMaximumPoolSize(poolSize);
        ds.setMaxLifetime(maxLifetime * 1000);


        // Database URL
        ds.setJdbcUrl("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database);

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
                + col.ID + " MEDIUMINT(8) UNSIGNED AUTO_INCREMENT,"
                + "PRIMARY KEY (" + col.ID + ")"
                + ") CHARACTER SET = utf8;";
            st.executeUpdate(sql);

            DatabaseMetaData md = con.getMetaData();
            if (isColumnMissing(md, col.NAME)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.NAME + " VARCHAR(255) NOT NULL UNIQUE AFTER " + col.ID + ";");
            }

            if (isColumnMissing(md, col.REAL_NAME)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.REAL_NAME + " VARCHAR(255) NOT NULL AFTER " + col.NAME + ";");
            }

            if (isColumnMissing(md, col.PASSWORD)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.PASSWORD + " VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL;");
            }

            if (!col.SALT.isEmpty() && isColumnMissing(md, col.SALT)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + col.SALT + " VARCHAR(255);");
            }

            if (isColumnMissing(md, col.LAST_IP)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.LAST_IP + " VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL;");
            }

            if (isColumnMissing(md, col.LAST_LOGIN)) {
                // TODO #792: Last login should be nullable
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.LAST_LOGIN + " BIGINT NOT NULL DEFAULT 0;");
            } else {
                migrateLastLoginColumn(con, md);
            }

            if (isColumnMissing(md, col.REGISTRATION_DATE)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.REGISTRATION_DATE + " BIGINT;");
            }

            if (isColumnMissing(md, col.REGISTRATION_IP)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.REGISTRATION_IP + " VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin;");
            }

            if (isColumnMissing(md, col.LASTLOC_X)) {
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

            if (isColumnMissing(md, col.LASTLOC_WORLD)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.LASTLOC_WORLD + " VARCHAR(255) NOT NULL DEFAULT 'world' AFTER " + col.LASTLOC_Z);
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
                    + col.EMAIL + " VARCHAR(255) DEFAULT 'your@email.com' AFTER " + col.LASTLOC_WORLD);
            }

            if (isColumnMissing(md, col.IS_LOGGED)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.IS_LOGGED + " SMALLINT NOT NULL DEFAULT '0' AFTER " + col.EMAIL);
            }
        }
        ConsoleLogger.info("MySQL setup finished");
    }

    private boolean isColumnMissing(DatabaseMetaData metaData, String columnName) throws SQLException {
        try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
            return !rs.next();
        }
    }

    @Override
    public boolean isAuthAvailable(String user) {
        String sql = "SELECT " + col.NAME + " FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user.toLowerCase());
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public HashedPassword getPassword(String user) {
        boolean useSalt = !col.SALT.isEmpty();
        String sql = "SELECT " + col.PASSWORD
            + (useSalt ? ", " + col.SALT : "")
            + " FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user.toLowerCase());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return new HashedPassword(rs.getString(col.PASSWORD),
                        useSalt ? rs.getString(col.SALT) : null);
                }
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return null;
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
        try (Connection con = getConnection()) {
            String sql;

            // TODO #792: Last login and last IP should be NULL here
            boolean useSalt = !col.SALT.isEmpty() || !StringUtils.isEmpty(auth.getPassword().getSalt());
            sql = "INSERT INTO " + tableName + "("
                + col.NAME + "," + col.PASSWORD + "," + col.LAST_IP + "," + col.LAST_LOGIN + "," + col.REAL_NAME
                + "," + col.EMAIL + "," + col.REGISTRATION_DATE + "," + col.REGISTRATION_IP
                + (useSalt ? "," + col.SALT : "")
                + ") VALUES (?,?,?,?,?,?,?,?" + (useSalt ? ",?" : "") + ");";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, auth.getNickname());
                pst.setString(2, auth.getPassword().getHash());
                pst.setString(3, auth.getLastIp());
                pst.setLong(4, auth.getLastLogin());
                pst.setString(5, auth.getRealName());
                pst.setString(6, auth.getEmail());
                pst.setLong(7, System.currentTimeMillis());
                pst.setString(8, auth.getRegistrationIp());
                if (useSalt) {
                    pst.setString(9, auth.getPassword().getSalt());
                }
                pst.executeUpdate();
            }

            if (!columnOthers.isEmpty()) {
                for (String column : columnOthers) {
                    try (PreparedStatement pst = con.prepareStatement("UPDATE " + tableName + " SET " + column + "=? WHERE " + col.NAME + "=?;")) {
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
    public boolean updatePassword(PlayerAuth auth) {
        return updatePassword(auth.getNickname(), auth.getPassword());
    }

    @Override
    public boolean updatePassword(String user, HashedPassword password) {
        user = user.toLowerCase();
        try (Connection con = getConnection()) {
            boolean useSalt = !col.SALT.isEmpty();
            if (useSalt) {
                String sql = String.format("UPDATE %s SET %s = ?, %s = ? WHERE %s = ?;",
                    tableName, col.PASSWORD, col.SALT, col.NAME);
                try (PreparedStatement pst = con.prepareStatement(sql)) {
                    pst.setString(1, password.getHash());
                    pst.setString(2, password.getSalt());
                    pst.setString(3, user);
                    pst.executeUpdate();
                }
            } else {
                String sql = String.format("UPDATE %s SET %s = ? WHERE %s = ?;",
                    tableName, col.PASSWORD, col.NAME);
                try (PreparedStatement pst = con.prepareStatement(sql)) {
                    pst.setString(1, password.getHash());
                    pst.setString(2, user);
                    pst.executeUpdate();
                }
            }
            sqlExtension.changePassword(user, password, con);
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public boolean updateSession(PlayerAuth auth) {
        String sql = "UPDATE " + tableName + " SET "
            + col.LAST_IP + "=?, " + col.LAST_LOGIN + "=?, " + col.REAL_NAME + "=? WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, auth.getLastIp());
            pst.setLong(2, auth.getLastLogin());
            pst.setString(3, auth.getRealName());
            pst.setString(4, auth.getNickname());
            pst.executeUpdate();
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public Set<String> getRecordsToPurge(long until, boolean includeEntriesWithLastLoginZero) {
        Set<String> list = new HashSet<>();
        String select = "SELECT " + col.NAME + " FROM " + tableName + " WHERE " + col.LAST_LOGIN + " < ?";
        if (!includeEntriesWithLastLoginZero) {
            select += " AND " + col.LAST_LOGIN + " <> 0";
        }
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
    public boolean updateQuitLoc(PlayerAuth auth) {
        String sql = "UPDATE " + tableName
            + " SET " + col.LASTLOC_X + " =?, " + col.LASTLOC_Y + "=?, " + col.LASTLOC_Z + "=?, "
            + col.LASTLOC_WORLD + "=?, " + col.LASTLOC_YAW + "=?, " + col.LASTLOC_PITCH + "=?"
            + " WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setDouble(1, auth.getQuitLocX());
            pst.setDouble(2, auth.getQuitLocY());
            pst.setDouble(3, auth.getQuitLocZ());
            pst.setString(4, auth.getWorld());
            pst.setFloat(5, auth.getYaw());
            pst.setFloat(6, auth.getPitch());
            pst.setString(7, auth.getNickname());
            pst.executeUpdate();
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public boolean updateEmail(PlayerAuth auth) {
        String sql = "UPDATE " + tableName + " SET " + col.EMAIL + " =? WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, auth.getEmail());
            pst.setString(2, auth.getNickname());
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
    public List<String> getAllAuthsByIp(String ip) {
        List<String> result = new ArrayList<>();
        String sql = "SELECT " + col.NAME + " FROM " + tableName + " WHERE " + col.LAST_IP + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, ip);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString(col.NAME));
                }
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return result;
    }

    @Override
    public int countAuthsByEmail(String email) {
        String sql = "SELECT COUNT(1) FROM " + tableName + " WHERE UPPER(" + col.EMAIL + ") = UPPER(?)";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, email);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return 0;
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
        return DataSourceType.MYSQL;
    }

    @Override
    public boolean isLogged(String user) {
        String sql = "SELECT " + col.IS_LOGGED + " FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() && (rs.getInt(col.IS_LOGGED) == 1);
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public void setLogged(String user) {
        String sql = "UPDATE " + tableName + " SET " + col.IS_LOGGED + "=? WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, 1);
            pst.setString(2, user.toLowerCase());
            pst.executeUpdate();
        } catch (SQLException ex) {
            logSqlException(ex);
        }
    }

    @Override
    public void setUnlogged(String user) {
        String sql = "UPDATE " + tableName + " SET " + col.IS_LOGGED + "=? WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, 0);
            pst.setString(2, user.toLowerCase());
            pst.executeUpdate();
        } catch (SQLException ex) {
            logSqlException(ex);
        }
    }

    @Override
    public void purgeLogged() {
        String sql = "UPDATE " + tableName + " SET " + col.IS_LOGGED + "=? WHERE " + col.IS_LOGGED + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, 0);
            pst.setInt(2, 1);
            pst.executeUpdate();
        } catch (SQLException ex) {
            logSqlException(ex);
        }
    }

    @Override
    public int getAccountsRegistered() {
        int result = 0;
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                result = rs.getInt(1);
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return result;
    }

    @Override
    public boolean updateRealName(String user, String realName) {
        String sql = "UPDATE " + tableName + " SET " + col.REAL_NAME + "=? WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, realName);
            pst.setString(2, user);
            pst.executeUpdate();
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public DataSourceResult<String> getEmail(String user) {
        String sql = "SELECT " + col.EMAIL + " FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return DataSourceResult.of(rs.getString(1));
                }
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return DataSourceResult.unknownPlayer();
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

    private PlayerAuth buildAuthFromResultSet(ResultSet row) throws SQLException {
        String salt = col.SALT.isEmpty() ? null : row.getString(col.SALT);
        int group = col.GROUP.isEmpty() ? -1 : row.getInt(col.GROUP);
        return PlayerAuth.builder()
            .name(row.getString(col.NAME))
            .realName(row.getString(col.REAL_NAME))
            .password(row.getString(col.PASSWORD), salt)
            .lastLogin(row.getLong(col.LAST_LOGIN))
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

    /**
     * Checks if the last login column has a type that needs to be migrated.
     *
     * @param con      connection to the database
     * @param metaData lastlogin column meta data
     *
     * @throws SQLException .
     */
    private void migrateLastLoginColumn(Connection con, DatabaseMetaData metaData) throws SQLException {
        final int columnType;
        try (ResultSet rs = metaData.getColumns(null, null, tableName, col.LAST_LOGIN)) {
            if (!rs.next()) {
                ConsoleLogger.warning("Could not get LAST_LOGIN meta data. This should never happen!");
                return;
            }
            columnType = rs.getInt("DATA_TYPE");
        }

        if (columnType == Types.TIMESTAMP) {
            migrateLastLoginColumnFromTimestamp(con);
        } else if (columnType == Types.INTEGER) {
            migrateLastLoginColumnFromInt(con);
        }
    }

    /**
     * Performs conversion of lastlogin column from timestamp type to bigint.
     *
     * @param con connection to the database
     */
    private void migrateLastLoginColumnFromTimestamp(Connection con) throws SQLException {
        ConsoleLogger.info("Migrating lastlogin column from timestamp to bigint");
        final String lastLoginOld = col.LAST_LOGIN + "_old";

        // Rename lastlogin to lastlogin_old
        String sql = String.format("ALTER TABLE %s CHANGE COLUMN %s %s BIGINT",
            tableName, col.LAST_LOGIN, lastLoginOld);
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.execute();
        }

        // Create lastlogin column
        sql = String.format("ALTER TABLE %s ADD COLUMN %s "
                + "BIGINT NOT NULL DEFAULT 0 AFTER %s",
            tableName, col.LAST_LOGIN, col.LAST_IP);
        con.prepareStatement(sql).execute();

        // Set values of lastlogin based on lastlogin_old
        sql = String.format("UPDATE %s SET %s = UNIX_TIMESTAMP(%s) * 1000",
            tableName, col.LAST_LOGIN, lastLoginOld);
        con.prepareStatement(sql).execute();

        // Drop lastlogin_old
        sql = String.format("ALTER TABLE %s DROP COLUMN %s",
            tableName, lastLoginOld);
        con.prepareStatement(sql).execute();
        ConsoleLogger.info("Finished migration of lastlogin (timestamp to bigint)");
    }

    /**
     * Performs conversion of lastlogin column from int to bigint.
     *
     * @param con connection to the database
     */
    private void migrateLastLoginColumnFromInt(Connection con) throws SQLException {
        // Change from int to bigint
        ConsoleLogger.info("Migrating lastlogin column from int to bigint");
        String sql = String.format("ALTER TABLE %s MODIFY %s BIGINT;", tableName, col.LAST_LOGIN);
        con.prepareStatement(sql).execute();

        // Migrate timestamps in seconds format to milliseconds format if they are plausible
        int rangeStart = 1262304000; // timestamp for 2010-01-01
        int rangeEnd = 1514678400;   // timestamp for 2017-12-31
        sql = String.format("UPDATE %s SET %s = %s * 1000 WHERE %s > %d AND %s < %d;",
            tableName, col.LAST_LOGIN, col.LAST_LOGIN, col.LAST_LOGIN, rangeStart, col.LAST_LOGIN, rangeEnd);
        int changedRows = con.prepareStatement(sql).executeUpdate();

        ConsoleLogger.warning("You may have entries with invalid timestamps. Please check your data "
            + "before purging. " + changedRows + " rows were migrated from seconds to milliseconds.");
    }
}
