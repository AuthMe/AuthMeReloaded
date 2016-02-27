package fr.xephi.authme.datasource;

import com.google.common.annotations.VisibleForTesting;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool.PoolInitializationException;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.security.crypts.XFBCRYPT;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.StringUtils;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class MySQL implements DataSource {

    private final String host;
    private final String port;
    private final String username;
    private final String password;
    private final String database;
    private final String tableName;
    private final List<String> columnOthers;
    private final Columns col;
    private final HashAlgorithm hashAlgorithm;
    private HikariDataSource ds;

    public MySQL(NewSetting settings) throws ClassNotFoundException, SQLException, PoolInitializationException {
        this.host = settings.getProperty(DatabaseSettings.MYSQL_HOST);
        this.port = settings.getProperty(DatabaseSettings.MYSQL_PORT);
        this.username = settings.getProperty(DatabaseSettings.MYSQL_USERNAME);
        this.password = settings.getProperty(DatabaseSettings.MYSQL_PASSWORD);
        this.database = settings.getProperty(DatabaseSettings.MYSQL_DATABASE);
        this.tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);
        this.columnOthers = settings.getProperty(HooksSettings.MYSQL_OTHER_USERNAME_COLS);
        this.col = new Columns(settings);
        this.hashAlgorithm = settings.getProperty(SecuritySettings.PASSWORD_HASH);

        // Set the connection arguments (and check if connection is ok)
        try {
            this.setConnectionArguments();
        } catch (RuntimeException e) {
            if (e instanceof IllegalArgumentException) {
                ConsoleLogger.showError("Invalid database arguments! Please check your configuration!");
                ConsoleLogger.showError("If this error persists, please report it to the developer!");
                throw new IllegalArgumentException(e);
            }
            if (e instanceof PoolInitializationException) {
                ConsoleLogger.showError("Can't initialize database connection! Please check your configuration!");
                ConsoleLogger.showError("If this error persists, please report it to the developer!");
                throw new PoolInitializationException(e);
            }
            ConsoleLogger.showError("Can't use the Hikari Connection Pool! Please, report this error to the developer!");
            throw e;
        }

        // Initialize the database
        try {
            this.setupConnection();
        } catch (SQLException e) {
            this.close();
            ConsoleLogger.showError("Can't initialize the MySQL database... Please check your database settings in the config.yml file! SHUTDOWN...");
            ConsoleLogger.showError("If this error persists, please report it to the developer!");
            throw e;
        }
    }

    @VisibleForTesting
    MySQL(NewSetting settings, HikariDataSource hikariDataSource) {
        this.host = settings.getProperty(DatabaseSettings.MYSQL_HOST);
        this.port = settings.getProperty(DatabaseSettings.MYSQL_PORT);
        this.username = settings.getProperty(DatabaseSettings.MYSQL_USERNAME);
        this.password = settings.getProperty(DatabaseSettings.MYSQL_PASSWORD);
        this.database = settings.getProperty(DatabaseSettings.MYSQL_DATABASE);
        this.tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);
        this.columnOthers = settings.getProperty(HooksSettings.MYSQL_OTHER_USERNAME_COLS);
        this.col = new Columns(settings);
        this.hashAlgorithm = settings.getProperty(SecuritySettings.PASSWORD_HASH);
        ds = hikariDataSource;
    }

    private synchronized void setConnectionArguments() throws RuntimeException {
        ds = new HikariDataSource();
        ds.setPoolName("AuthMeMYSQLPool");
        ds.setDriverClassName("com.mysql.jdbc.Driver");
        ds.setJdbcUrl("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database);
        ds.addDataSourceProperty("rewriteBatchedStatements", "true");
        ds.addDataSourceProperty("jdbcCompliantTruncation", "false");
        ds.addDataSourceProperty("cachePrepStmts", "true");
        ds.addDataSourceProperty("prepStmtCacheSize", "250");
        ds.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        //set utf-8 as default encoding
        ds.addDataSourceProperty("characterEncoding", "utf8");
        ds.addDataSourceProperty("encoding","UTF-8");
        ds.addDataSourceProperty("useUnicode", "true");

        ds.setUsername(this.username);
        ds.setPassword(this.password);
        ds.setInitializationFailFast(true); // Don't start the plugin if the database is unavailable
        ds.setMaxLifetime(180000); // 3 Min
        ds.setIdleTimeout(60000); // 1 Min
        ds.setMinimumIdle(2);
        ds.setMaximumPoolSize((Runtime.getRuntime().availableProcessors() * 2) + 1);
        ConsoleLogger.info("Connection arguments loaded, Hikari ConnectionPool ready!");
    }

    private synchronized void reloadArguments() throws RuntimeException {
        if (ds != null) {
            ds.close();
        }
        setConnectionArguments();
        ConsoleLogger.info("Hikari ConnectionPool arguments reloaded!");
    }

    private synchronized Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    private synchronized void setupConnection() throws SQLException {
        try (Connection con = getConnection()) {
            Statement st = con.createStatement();
            DatabaseMetaData md = con.getMetaData();
            // Create table if not exists.
            String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + col.ID + " INTEGER AUTO_INCREMENT,"
                + col.NAME + " VARCHAR(255) NOT NULL UNIQUE,"
                + col.REAL_NAME + " VARCHAR(255) NOT NULL,"
                + col.PASSWORD + " VARCHAR(255) NOT NULL,"
                + col.IP + " VARCHAR(40) NOT NULL DEFAULT '127.0.0.1',"
                + col.LAST_LOGIN + " BIGINT NOT NULL DEFAULT 0,"
                + col.LASTLOC_X + " DOUBLE NOT NULL DEFAULT '0.0',"
                + col.LASTLOC_Y + " DOUBLE NOT NULL DEFAULT '0.0',"
                + col.LASTLOC_Z + " DOUBLE NOT NULL DEFAULT '0.0',"
                + col.LASTLOC_WORLD + " VARCHAR(255) NOT NULL DEFAULT '" + Settings.defaultWorld + "',"
                + col.EMAIL + " VARCHAR(255) DEFAULT 'your@email.com',"
                + col.IS_LOGGED + " SMALLINT NOT NULL DEFAULT '0',"
                + "CONSTRAINT table_const_prim PRIMARY KEY (" + col.ID + ")"
                + ");";
            st.executeUpdate(sql);

            ResultSet rs = md.getColumns(null, null, tableName, col.NAME);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.NAME + " VARCHAR(255) NOT NULL UNIQUE AFTER " + col.ID + ";");
            }
            rs.close();

            rs = md.getColumns(null, null, tableName, col.REAL_NAME);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.REAL_NAME + " VARCHAR(255) NOT NULL AFTER " + col.NAME + ";");
            }
            rs.close();

            rs = md.getColumns(null, null, tableName, col.PASSWORD);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.PASSWORD + " VARCHAR(255) NOT NULL;");
            }
            rs.close();

            if (!col.SALT.isEmpty()) {
                rs = md.getColumns(null, null, tableName, col.SALT);
                if (!rs.next()) {
                    st.executeUpdate("ALTER TABLE " + tableName
                        + " ADD COLUMN " + col.SALT + " VARCHAR(255);");
                }
                rs.close();
            }

            rs = md.getColumns(null, null, tableName, col.IP);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.IP + " VARCHAR(40) NOT NULL;");
            }
            rs.close();

            rs = md.getColumns(null, null, tableName, col.LAST_LOGIN);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.LAST_LOGIN + " BIGINT NOT NULL DEFAULT 0;");
            } else {
                migrateLastLoginColumnToBigInt(con, rs);
            }
            rs.close();

            rs = md.getColumns(null, null, tableName, col.LASTLOC_X);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.LASTLOC_X + " DOUBLE NOT NULL DEFAULT '0.0' AFTER " + col.LAST_LOGIN + " , ADD "
                    + col.LASTLOC_Y + " DOUBLE NOT NULL DEFAULT '0.0' AFTER " + col.LASTLOC_X + " , ADD "
                    + col.LASTLOC_Z + " DOUBLE NOT NULL DEFAULT '0.0' AFTER " + col.LASTLOC_Y);
            }
            rs.close();

            rs = md.getColumns(null, null, tableName, col.LASTLOC_X);
            if (rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " MODIFY "
                    + col.LASTLOC_X + " DOUBLE NOT NULL DEFAULT '0.0', MODIFY "
                    + col.LASTLOC_Y + " DOUBLE NOT NULL DEFAULT '0.0', MODIFY "
                    + col.LASTLOC_Z + " DOUBLE NOT NULL DEFAULT '0.0';");
            }
            rs.close();

            rs = md.getColumns(null, null, tableName, col.LASTLOC_WORLD);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.LASTLOC_WORLD + " VARCHAR(255) NOT NULL DEFAULT 'world' AFTER " + col.LASTLOC_Z);
            }
            rs.close();

            rs = md.getColumns(null, null, tableName, col.EMAIL);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.EMAIL + " VARCHAR(255) DEFAULT 'your@email.com' AFTER " + col.LASTLOC_WORLD);
            }
            rs.close();

            rs = md.getColumns(null, null, tableName, col.IS_LOGGED);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.IS_LOGGED + " SMALLINT NOT NULL DEFAULT '0' AFTER " + col.EMAIL);
            }
            rs.close();

            st.close();
        }
        ConsoleLogger.info("MySQL setup finished");
    }

    @Override
    public synchronized boolean isAuthAvailable(String user) {
        String sql = "SELECT " + col.NAME + " FROM " + tableName + " WHERE " + col.NAME + "=?;";
        ResultSet rs = null;
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user.toLowerCase());
            rs = pst.executeQuery();
            return rs.next();
        } catch (SQLException ex) {
            logSqlException(ex);
        } finally {
            close(rs);
        }
        return false;
    }

    @Override
    public HashedPassword getPassword(String user) {
        String sql = "SELECT " + col.PASSWORD + "," + col.SALT + " FROM " + tableName
            + " WHERE " + col.NAME + "=?;";
        ResultSet rs = null;
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user.toLowerCase());
            rs = pst.executeQuery();
            if (rs.next()) {
                return new HashedPassword(rs.getString(col.PASSWORD),
                    !col.SALT.isEmpty() ? rs.getString(col.SALT) : null);
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        } finally {
            close(rs);
        }
        return null;
    }

    @Override
    public synchronized PlayerAuth getAuth(String user) {
        String sql = "SELECT * FROM " + tableName + " WHERE " + col.NAME + "=?;";
        PlayerAuth auth;
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user.toLowerCase());
            int id;
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                id = rs.getInt(col.ID);
                auth = buildAuthFromResultSet(rs);
            }
            if (hashAlgorithm == HashAlgorithm.XFBCRYPT) {
                try (PreparedStatement pst2 = con.prepareStatement(
                    "SELECT data FROM xf_user_authenticate WHERE " + col.ID + "=?;")) {
                    pst2.setInt(1, id);
                    try (ResultSet rs = pst2.executeQuery()) {
                        if (rs.next()) {
                            Blob blob = rs.getBlob("data");
                            byte[] bytes = blob.getBytes(1, (int) blob.length());
                            auth.setPassword(new HashedPassword(XFBCRYPT.getHashFromBlob(bytes)));
                        }
                    }
                }
            }
            return auth;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return null;
    }

    @Override
    public synchronized boolean saveAuth(PlayerAuth auth) {
        try (Connection con = getConnection()) {
            PreparedStatement pst;
            PreparedStatement pst2;
            ResultSet rs;
            String sql;

            boolean useSalt = !col.SALT.isEmpty() || !StringUtils.isEmpty(auth.getPassword().getSalt());
            sql = "INSERT INTO " + tableName + "("
                + col.NAME + "," + col.PASSWORD + "," + col.IP + ","
                + col.LAST_LOGIN + "," + col.REAL_NAME + "," + col.EMAIL
                + (useSalt ? "," + col.SALT : "")
                + ") VALUES (?,?,?,?,?,?" + (useSalt ? ",?" : "") + ");";
            pst = con.prepareStatement(sql);
            pst.setString(1, auth.getNickname());
            pst.setString(2, auth.getPassword().getHash());
            pst.setString(3, auth.getIp());
            pst.setLong(4, auth.getLastLogin());
            pst.setString(5, auth.getRealName());
            pst.setString(6, auth.getEmail());
            if (useSalt) {
                pst.setString(7, auth.getPassword().getSalt());
            }
            pst.executeUpdate();
            pst.close();

            if (!columnOthers.isEmpty()) {
                for (String column : columnOthers) {
                    pst = con.prepareStatement("UPDATE " + tableName + " SET " + column + "=? WHERE " + col.NAME + "=?;");
                    pst.setString(1, auth.getRealName());
                    pst.setString(2, auth.getNickname());
                    pst.executeUpdate();
                    pst.close();
                }
            }

            if (hashAlgorithm == HashAlgorithm.PHPBB) {
                sql = "SELECT " + col.ID + " FROM " + tableName + " WHERE " + col.NAME + "=?;";
                pst = con.prepareStatement(sql);
                pst.setString(1, auth.getNickname());
                rs = pst.executeQuery();
                if (rs.next()) {
                    int id = rs.getInt(col.ID);
                    // Insert player in phpbb_user_group
                    sql = "INSERT INTO " + Settings.getPhpbbPrefix
                        + "user_group (group_id, user_id, group_leader, user_pending) VALUES (?,?,?,?);";
                    pst2 = con.prepareStatement(sql);
                    pst2.setInt(1, Settings.getPhpbbGroup);
                    pst2.setInt(2, id);
                    pst2.setInt(3, 0);
                    pst2.setInt(4, 0);
                    pst2.executeUpdate();
                    pst2.close();
                    // Update username_clean in phpbb_users
                    sql = "UPDATE " + tableName + " SET " + tableName
                        + ".username_clean=? WHERE " + col.NAME + "=?;";
                    pst2 = con.prepareStatement(sql);
                    pst2.setString(1, auth.getNickname());
                    pst2.setString(2, auth.getNickname());
                    pst2.executeUpdate();
                    pst2.close();
                    // Update player group in phpbb_users
                    sql = "UPDATE " + tableName + " SET " + tableName
                        + ".group_id=? WHERE " + col.NAME + "=?;";
                    pst2 = con.prepareStatement(sql);
                    pst2.setInt(1, Settings.getPhpbbGroup);
                    pst2.setString(2, auth.getNickname());
                    pst2.executeUpdate();
                    pst2.close();
                    // Get current time without ms
                    long time = System.currentTimeMillis() / 1000;
                    // Update user_regdate
                    sql = "UPDATE " + tableName + " SET " + tableName
                        + ".user_regdate=? WHERE " + col.NAME + "=?;";
                    pst2 = con.prepareStatement(sql);
                    pst2.setLong(1, time);
                    pst2.setString(2, auth.getNickname());
                    pst2.executeUpdate();
                    pst2.close();
                    // Update user_lastvisit
                    sql = "UPDATE " + tableName + " SET " + tableName
                        + ".user_lastvisit=? WHERE " + col.NAME + "=?;";
                    pst2 = con.prepareStatement(sql);
                    pst2.setLong(1, time);
                    pst2.setString(2, auth.getNickname());
                    pst2.executeUpdate();
                    pst2.close();
                    // Increment num_users
                    sql = "UPDATE " + Settings.getPhpbbPrefix
                        + "config SET config_value = config_value + 1 WHERE config_name = 'num_users';";
                    pst2 = con.prepareStatement(sql);
                    pst2.executeUpdate();
                    pst2.close();
                }
                rs.close();
                pst.close();
            } else if (hashAlgorithm == HashAlgorithm.WORDPRESS) {
                pst = con.prepareStatement("SELECT " + col.ID + " FROM " + tableName + " WHERE " + col.NAME + "=?;");
                pst.setString(1, auth.getNickname());
                rs = pst.executeQuery();
                if (rs.next()) {
                    int id = rs.getInt(col.ID);
                    sql = "INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);";
                    pst2 = con.prepareStatement(sql);
                    // First Name
                    pst2.setInt(1, id);
                    pst2.setString(2, "first_name");
                    pst2.setString(3, "");
                    pst2.addBatch();
                    // Last Name
                    pst2.setInt(1, id);
                    pst2.setString(2, "last_name");
                    pst2.setString(3, "");
                    pst2.addBatch();
                    // Nick Name
                    pst2.setInt(1, id);
                    pst2.setString(2, "nickname");
                    pst2.setString(3, auth.getNickname());
                    pst2.addBatch();
                    // Description
                    pst2.setInt(1, id);
                    pst2.setString(2, "description");
                    pst2.setString(3, "");
                    pst2.addBatch();
                    // Rich_Editing
                    pst2.setInt(1, id);
                    pst2.setString(2, "rich_editing");
                    pst2.setString(3, "true");
                    pst2.addBatch();
                    // Comments_Shortcuts
                    pst2.setInt(1, id);
                    pst2.setString(2, "comment_shortcuts");
                    pst2.setString(3, "false");
                    pst2.addBatch();
                    // admin_color
                    pst2.setInt(1, id);
                    pst2.setString(2, "admin_color");
                    pst2.setString(3, "fresh");
                    pst2.addBatch();
                    // use_ssl
                    pst2.setInt(1, id);
                    pst2.setString(2, "use_ssl");
                    pst2.setString(3, "0");
                    pst2.addBatch();
                    // show_admin_bar_front
                    pst2.setInt(1, id);
                    pst2.setString(2, "show_admin_bar_front");
                    pst2.setString(3, "true");
                    pst2.addBatch();
                    // wp_capabilities
                    pst2.setInt(1, id);
                    pst2.setString(2, "wp_capabilities");
                    pst2.setString(3, "a:1:{s:10:\"subscriber\";b:1;}");
                    pst2.addBatch();
                    // wp_user_level
                    pst2.setInt(1, id);
                    pst2.setString(2, "wp_user_level");
                    pst2.setString(3, "0");
                    pst2.addBatch();
                    // default_password_nag
                    pst2.setInt(1, id);
                    pst2.setString(2, "default_password_nag");
                    pst2.setString(3, "");
                    pst2.addBatch();

                    // Execute queries
                    pst2.executeBatch();
                    pst2.clearBatch();
                    pst2.close();
                }
                rs.close();
                pst.close();
            } else if (hashAlgorithm == HashAlgorithm.XFBCRYPT) {
                pst = con.prepareStatement("SELECT " + col.ID + " FROM " + tableName + " WHERE " + col.NAME + "=?;");
                pst.setString(1, auth.getNickname());
                rs = pst.executeQuery();
                if (rs.next()) {
                    int id = rs.getInt(col.ID);
                    sql = "INSERT INTO xf_user_authenticate (user_id, scheme_class, data) VALUES (?,?,?)";
                    pst2 = con.prepareStatement(sql);
                    pst2.setInt(1, id);
                    pst2.setString(2, XFBCRYPT.SCHEME_CLASS);
                    String serializedHash = XFBCRYPT.serializeHash(auth.getPassword().getHash());
                    byte[] bytes = serializedHash.getBytes();
                    Blob blob = con.createBlob();
                    blob.setBytes(1, bytes);
                    pst2.setBlob(3, blob);
                    pst2.executeUpdate();
                    pst2.close();
                }
                rs.close();
                pst.close();
            }
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public synchronized boolean updatePassword(PlayerAuth auth) {
        return updatePassword(auth.getNickname(), auth.getPassword());
    }

    @Override
    public boolean updatePassword(String user, HashedPassword password) {
        user = user.toLowerCase();
        try (Connection con = getConnection()) {
            boolean useSalt = !col.SALT.isEmpty();
            PreparedStatement pst;
            if (useSalt) {
                String sql = String.format("UPDATE %s SET %s = ?, %s = ? WHERE %s = ?;",
                    tableName, col.PASSWORD, col.SALT, col.NAME);
                pst = con.prepareStatement(sql);
                pst.setString(1, password.getHash());
                pst.setString(2, password.getSalt());
                pst.setString(3, user);
            } else {
                String sql = String.format("UPDATE %s SET %s = ? WHERE %s = ?;",
                    tableName, col.PASSWORD, col.NAME);
                pst = con.prepareStatement(sql);
                pst.setString(1, password.getHash());
                pst.setString(2, user);
            }
            pst.executeUpdate();
            pst.close();
            if (hashAlgorithm == HashAlgorithm.XFBCRYPT) {
                String sql = "SELECT " + col.ID + " FROM " + tableName + " WHERE " + col.NAME + "=?;";
                pst = con.prepareStatement(sql);
                pst.setString(1, user);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    int id = rs.getInt(col.ID);
                    // Insert password in the correct table
                    sql = "UPDATE xf_user_authenticate SET data=? WHERE " + col.ID + "=?;";
                    PreparedStatement pst2 = con.prepareStatement(sql);
                    String serializedHash = XFBCRYPT.serializeHash(password.getHash());
                    byte[] bytes = serializedHash.getBytes();
                    Blob blob = con.createBlob();
                    blob.setBytes(1, bytes);
                    pst2.setBlob(1, blob);
                    pst2.setInt(2, id);
                    pst2.executeUpdate();
                    pst2.close();
                    // ...
                    sql = "UPDATE xf_user_authenticate SET scheme_class=? WHERE " + col.ID + "=?;";
                    pst2 = con.prepareStatement(sql);
                    pst2.setString(1, XFBCRYPT.SCHEME_CLASS);
                    pst2.setInt(2, id);
                    pst2.executeUpdate();
                    pst2.close();
                }
                rs.close();
                pst.close();
            }
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public synchronized boolean updateSession(PlayerAuth auth) {
        String sql = "UPDATE " + tableName + " SET "
            + col.IP + "=?, " + col.LAST_LOGIN + "=?, " + col.REAL_NAME + "=? WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, auth.getIp());
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
    public synchronized List<String> autoPurgeDatabase(long until) {
        List<String> list = new ArrayList<>();
        String select = "SELECT " + col.NAME + " FROM " + tableName + " WHERE " + col.LAST_LOGIN + "<?;";
        String delete = "DELETE FROM " + tableName + " WHERE " + col.LAST_LOGIN + "<?;";
        try (Connection con = getConnection();
             PreparedStatement selectPst = con.prepareStatement(select);
             PreparedStatement deletePst = con.prepareStatement(delete)) {
            selectPst.setLong(1, until);
            try (ResultSet rs = selectPst.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString(col.NAME));
                }
            }
            deletePst.setLong(1, until);
            deletePst.executeUpdate();
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return list;
    }

    @Override
    public synchronized boolean removeAuth(String user) {
        user = user.toLowerCase();
        try (Connection con = getConnection()) {
            String sql;
            PreparedStatement pst;
            if (hashAlgorithm == HashAlgorithm.XFBCRYPT) {
                sql = "SELECT " + col.ID + " FROM " + tableName + " WHERE " + col.NAME + "=?;";
                pst = con.prepareStatement(sql);
                pst.setString(1, user);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    int id = rs.getInt(col.ID);
                    sql = "DELETE FROM xf_user_authenticate WHERE " + col.ID + "=?;";
                    PreparedStatement st = con.prepareStatement(sql);
                    st.setInt(1, id);
                    st.executeUpdate();
                    st.close();
                }
                rs.close();
                pst.close();
            }
            pst = con.prepareStatement("DELETE FROM " + tableName + " WHERE " + col.NAME + "=?;");
            pst.setString(1, user);
            pst.executeUpdate();
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public synchronized boolean updateQuitLoc(PlayerAuth auth) {
        String sql = "UPDATE " + tableName
            + " SET " + col.LASTLOC_X + " =?, " + col.LASTLOC_Y + "=?, " + col.LASTLOC_Z + "=?, " + col.LASTLOC_WORLD + "=?"
            + " WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setDouble(1, auth.getQuitLocX());
            pst.setDouble(2, auth.getQuitLocY());
            pst.setDouble(3, auth.getQuitLocZ());
            pst.setString(4, auth.getWorld());
            pst.setString(5, auth.getNickname());
            pst.executeUpdate();
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public synchronized boolean updateEmail(PlayerAuth auth) {
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
    public void reload() {
        try {
            reloadArguments();
        } catch (Exception ex) {
            ConsoleLogger.logException("Can't reconnect to MySQL database... " +
                "Please check your MySQL configuration! Encountered", ex);
            AuthMe.getInstance().stopOrUnload();
        }
    }

    @Override
    public synchronized void close() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
    }

    @Override
    public synchronized List<String> getAllAuthsByIp(String ip) {
        List<String> result = new ArrayList<>();
        String sql = "SELECT " + col.NAME + " FROM " + tableName + " WHERE " + col.IP + "=?;";
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
    public synchronized int countAuthsByEmail(String email) {
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
    public synchronized void purgeBanned(List<String> banned) {
        String sql = "DELETE FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            for (String name : banned) {
                pst.setString(1, name);
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
    public boolean updateIp(String user, String ip) {
        String sql = "UPDATE " + tableName + " SET " + col.IP + "=? WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, ip);
            pst.setString(2, user);
            pst.executeUpdate();
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public List<PlayerAuth> getAllAuths() {
        List<PlayerAuth> auths = new ArrayList<>();
        try (Connection con = getConnection()) {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM " + tableName);
            while (rs.next()) {
                PlayerAuth pAuth = buildAuthFromResultSet(rs);
                if (hashAlgorithm == HashAlgorithm.XFBCRYPT) {
                    try (PreparedStatement pst = con.prepareStatement("SELECT data FROM xf_user_authenticate WHERE " + col.ID + "=?;")) {
                        int id = rs.getInt(col.ID);
                        pst.setInt(1, id);
                        ResultSet rs2 = pst.executeQuery();
                        if (rs2.next()) {
                            Blob blob = rs2.getBlob("data");
                            byte[] bytes = blob.getBytes(1, (int) blob.length());
                            pAuth.setPassword(new HashedPassword(XFBCRYPT.getHashFromBlob(bytes)));
                        }
                        rs2.close();
                    }
                }
                auths.add(pAuth);
            }
            rs.close();
            st.close();
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return auths;
    }

    @Override
    public List<PlayerAuth> getLoggedPlayers() {
        List<PlayerAuth> auths = new ArrayList<>();
        try (Connection con = getConnection()) {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM " + tableName + " WHERE " + col.IS_LOGGED + "=1;");
            PreparedStatement pst = con.prepareStatement("SELECT data FROM xf_user_authenticate WHERE " + col.ID + "=?;");
            while (rs.next()) {
                PlayerAuth pAuth = buildAuthFromResultSet(rs);
                if (hashAlgorithm == HashAlgorithm.XFBCRYPT) {
                    int id = rs.getInt(col.ID);
                    pst.setInt(1, id);
                    ResultSet rs2 = pst.executeQuery();
                    if (rs2.next()) {
                        Blob blob = rs2.getBlob("data");
                        byte[] bytes = blob.getBytes(1, (int) blob.length());
                        pAuth.setPassword(new HashedPassword(XFBCRYPT.getHashFromBlob(bytes)));
                    }
                    rs2.close();
                }
                auths.add(pAuth);
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return auths;
    }

    @Override
    public synchronized boolean isEmailStored(String email) {
        String sql = "SELECT 1 FROM " + tableName + " WHERE UPPER(" + col.EMAIL + ") = UPPER(?)";
        try (Connection con = ds.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, email);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logSqlException(e);
        }
        return false;
    }

    private PlayerAuth buildAuthFromResultSet(ResultSet row) throws SQLException {
        String salt = col.SALT.isEmpty() ? null : row.getString(col.SALT);
        int group = col.GROUP.isEmpty() ? -1 : row.getInt(col.GROUP);
        return PlayerAuth.builder()
            .name(row.getString(col.NAME))
            .realName(row.getString(col.REAL_NAME))
            .password(row.getString(col.PASSWORD), salt)
            .lastLogin(row.getLong(col.LAST_LOGIN))
            .ip(row.getString(col.IP))
            .locWorld(row.getString(col.LASTLOC_WORLD))
            .locX(row.getDouble(col.LASTLOC_X))
            .locY(row.getDouble(col.LASTLOC_Y))
            .locZ(row.getDouble(col.LASTLOC_Z))
            .email(row.getString(col.EMAIL))
            .groupId(group)
            .build();
    }

    /**
     * Check if the lastlogin column is of type timestamp and, if so, revert it to the bigint format.
     *
     * @param con Connection to the database
     * @param rs ResultSet containing meta data for the lastlogin column
     */
    private void migrateLastLoginColumnToBigInt(Connection con, ResultSet rs) throws SQLException {
        final int columnType = rs.getInt("DATA_TYPE");
        if (columnType == Types.TIMESTAMP) {
            ConsoleLogger.info("Migrating lastlogin column from timestamp to bigint");
            final String lastLoginOld = col.LAST_LOGIN + "_old";

            // Rename lastlogin to lastlogin_old
            String sql = String.format("ALTER TABLE %s CHANGE COLUMN %s %s BIGINT",
                tableName, col.LAST_LOGIN, lastLoginOld);
            PreparedStatement pst = con.prepareStatement(sql);
            pst.execute();

            // Create lastlogin column
            sql = String.format("ALTER TABLE %s ADD COLUMN %s "
                    + "BIGINT NOT NULL DEFAULT 0 AFTER %s",
                tableName, col.LAST_LOGIN, col.IP);
            con.prepareStatement(sql).execute();

            // Set values of lastlogin based on lastlogin_old
            sql = String.format("UPDATE %s SET %s = UNIX_TIMESTAMP(%s)",
                tableName, col.LAST_LOGIN, lastLoginOld);
            con.prepareStatement(sql).execute();

            // Drop lastlogin_old
            sql = String.format("ALTER TABLE %s DROP COLUMN %s",
                tableName, lastLoginOld);
            con.prepareStatement(sql).execute();
            ConsoleLogger.info("Finished migration of lastlogin (timestamp to bigint)");
        }
    }

    private static void logSqlException(SQLException e) {
        ConsoleLogger.logException("Error during SQL operation:", e);
    }

    private static void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                ConsoleLogger.logException("Could not close ResultSet", e);
            }
        }
    }

}
