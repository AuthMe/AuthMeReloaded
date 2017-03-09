package fr.xephi.authme.datasource;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.security.crypts.XFBCRYPT;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.StringUtils;
import fr.xephi.authme.util.Utils;

import java.sql.Blob;
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

/**
 * SQL data source hooking into a Xenforo table.
 */
public class XenforoDataSource implements DataSource {

    private boolean useSSL;
    private String host;
    private String port;
    private String username;
    private String password;
    private String database;
    private String tableName;
    private int poolSize;
    private List<String> columnOthers;
    private Columns col;
    private HashAlgorithm hashAlgorithm;
    private HikariDataSource ds;
    private int xfGroup;


    public XenforoDataSource(Settings settings) throws ClassNotFoundException, SQLException {
        setParameters(settings);

        // Set the connection arguments (and check if connection is ok)
        try {
            this.setConnectionArguments();
        } catch (RuntimeException e) {
            if (e instanceof IllegalArgumentException) {
                ConsoleLogger.warning("Invalid database arguments! Please check your configuration!");
                ConsoleLogger.warning("If this error persists, please report it to the developer!");
            }
            if (e instanceof HikariPool.PoolInitializationException) {
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
            close();
            ConsoleLogger.logException("Can't initialize the MySQL database:", e);
            ConsoleLogger.warning("Please check your database settings in the config.yml file!");
            throw e;
        }
    }

    private void setParameters(Settings settings) {
        this.host = settings.getProperty(DatabaseSettings.MYSQL_HOST);
        this.port = settings.getProperty(DatabaseSettings.MYSQL_PORT);
        this.username = settings.getProperty(DatabaseSettings.MYSQL_USERNAME);
        this.password = settings.getProperty(DatabaseSettings.MYSQL_PASSWORD);
        this.database = settings.getProperty(DatabaseSettings.MYSQL_DATABASE);
        this.tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);
        this.columnOthers = settings.getProperty(HooksSettings.MYSQL_OTHER_USERNAME_COLS);
        this.col = new Columns(settings);
        this.hashAlgorithm = settings.getProperty(SecuritySettings.PASSWORD_HASH);
        this.xfGroup = settings.getProperty(HooksSettings.XF_ACTIVATED_GROUP_ID);
        this.poolSize = settings.getProperty(DatabaseSettings.MYSQL_POOL_SIZE);
        if (poolSize == -1) {
            poolSize = Utils.getCoreCount()*3;
        }
        this.useSSL = settings.getProperty(DatabaseSettings.MYSQL_USE_SSL);
    }

    private void setConnectionArguments() {
        ds = new HikariDataSource();
        ds.setPoolName("AuthMeMYSQLPool");

        // Pool size
        ds.setMaximumPoolSize(poolSize);

        // Database URL
        ds.setJdbcUrl("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database);

        // Auth
        ds.setUsername(this.username);
        ds.setPassword(this.password);

        // Request mysql over SSL
        ds.addDataSourceProperty("useSSL", useSSL);

        // Encoding
        ds.addDataSourceProperty("characterEncoding", "utf8");
        ds.addDataSourceProperty("encoding","UTF-8");
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

            if (isColumnMissing(md, col.IP)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.IP + " VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL;");
            }

            if (isColumnMissing(md, col.LAST_LOGIN)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.LAST_LOGIN + " BIGINT NOT NULL DEFAULT 0;");
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
            int id;
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                id = rs.getInt(col.ID);
                auth = buildAuthFromResultSet(rs);
            }

            // START XENFORO SPECIFIC
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
            // END XENFORO SPECIFIC

            return auth;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return null;
    }

    @Override
    public boolean saveAuth(PlayerAuth auth) {
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
            // START XENFORO SPECIFIC
            pst = con.prepareStatement("SELECT " + col.ID + " FROM " + tableName + " WHERE " + col.NAME + "=?;");
            pst.setString(1, auth.getNickname());
            rs = pst.executeQuery();
            if (rs.next()) {
                int id = rs.getInt(col.ID);
                // Insert player password, salt in xf_user_authenticate
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
                // Update player group in xf_users
                sql = "UPDATE " + tableName + " SET "+ tableName + ".user_group_id=? WHERE " + col.NAME + "=?;";
                pst2 = con.prepareStatement(sql);
                pst2.setInt(1, xfGroup);
                pst2.setString(2, auth.getNickname());
                pst2.executeUpdate();
                pst2.close();
                // Update player permission combination in xf_users
                sql = "UPDATE " + tableName + " SET "+ tableName + ".permission_combination_id=? WHERE " + col.NAME + "=?;";
                pst2 = con.prepareStatement(sql);
                pst2.setInt(1, xfGroup);
                pst2.setString(2, auth.getNickname());
                pst2.executeUpdate();
                pst2.close();
                // Insert player privacy combination in xf_user_privacy
                sql = "INSERT INTO xf_user_privacy (user_id, allow_view_profile, allow_post_profile, allow_send_personal_conversation, allow_view_identities, allow_receive_news_feed) VALUES (?,?,?,?,?,?)";
                pst2 = con.prepareStatement(sql);
                pst2.setInt(1, id);
                pst2.setString(2, "everyone");
                pst2.setString(3, "members");
                pst2.setString(4, "members");
                pst2.setString(5, "everyone");
                pst2.setString(6, "everyone");
                pst2.executeUpdate();
                pst2.close();
                // Insert player group relation in xf_user_group_relation
                sql = "INSERT INTO xf_user_group_relation (user_id, user_group_id, is_primary) VALUES (?,?,?)";
                pst2 = con.prepareStatement(sql);
                pst2.setInt(1, id);
                pst2.setInt(2, xfGroup);
                pst2.setString(3, "1");
                pst2.executeUpdate();
                pst2.close();
            }
            rs.close();
            pst.close();
            // END XENFORO SPECIFIC

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
    public boolean updateSession(PlayerAuth auth) {
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
        PreparedStatement xfSelect = null;
        PreparedStatement xfDelete = null;
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            if (hashAlgorithm == HashAlgorithm.XFBCRYPT) {
                sql = "SELECT " + col.ID + " FROM " + tableName + " WHERE " + col.NAME + "=?;";
                xfSelect = con.prepareStatement(sql);
                xfSelect.setString(1, user);
                try (ResultSet rs = xfSelect.executeQuery()) {
                    if (rs.next()) {
                        int id = rs.getInt(col.ID);
                        sql = "DELETE FROM xf_user_authenticate WHERE " + col.ID + "=?;";
                        xfDelete = con.prepareStatement(sql);
                        xfDelete.setInt(1, id);
                        xfDelete.executeUpdate();
                    }
                }
            }
            pst.setString(1, user.toLowerCase());
            pst.executeUpdate();
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        } finally {
            close(xfSelect);
            close(xfDelete);
        }
        return false;
    }

    @Override
    public boolean updateQuitLoc(PlayerAuth auth) {
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
    public void close() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
    }

    @Override
    public List<String> getAllAuthsByIp(String ip) {
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
        return DataSourceType.XENFORO_SQL;
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
        String sql = "SELECT * FROM " + tableName + " WHERE " + col.IS_LOGGED + "=1;";
        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                PlayerAuth pAuth = buildAuthFromResultSet(rs);

                // START XENFORO SPECIFIC
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
                // END XENFORO SPECIFIC

                auths.add(pAuth);
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return auths;
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

    private static void logSqlException(SQLException e) {
        ConsoleLogger.logException("Error during SQL operation:", e);
    }

    private static void close(ResultSet rs) {
        try {
            if (rs != null && !rs.isClosed()) {
                rs.close();
            }
        } catch (SQLException e) {
            ConsoleLogger.logException("Could not close ResultSet", e);
        }
    }

    private static void close(PreparedStatement pst) {
        try {
            if (pst != null && !pst.isClosed()) {
                pst.close();
            }
        } catch (SQLException e) {
            ConsoleLogger.logException("Could not close PreparedStatement", e);
        }
    }

}
