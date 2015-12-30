package fr.xephi.authme.datasource;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool.PoolInitializationException;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.queries.Query;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.settings.Settings;

import java.sql.*;
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
    private final String columnName;
    private final String columnPassword;
    private final String columnIp;
    private final String columnLastLogin;
    private final String columnSalt;
    private final String columnGroup;
    private final String lastlocX;
    private final String lastlocY;
    private final String lastlocZ;
    private final String lastlocWorld;
    private final String columnEmail;
    private final String columnID;
    private final String columnLogged;
    private final String columnRealName;
    private final List<String> columnOthers;
    private HikariDataSource ds;

    /**
     * Constructor for MySQL.
     *
     * @throws ClassNotFoundException * @throws SQLException * @throws PoolInitializationException
     */
    public MySQL() throws ClassNotFoundException, SQLException, PoolInitializationException {
        this.host = Settings.getMySQLHost;
        this.port = Settings.getMySQLPort;
        this.username = Settings.getMySQLUsername;
        this.password = Settings.getMySQLPassword;
        this.database = Settings.getMySQLDatabase;
        this.tableName = Settings.getMySQLTablename;
        this.columnName = Settings.getMySQLColumnName;
        this.columnPassword = Settings.getMySQLColumnPassword;
        this.columnIp = Settings.getMySQLColumnIp;
        this.columnLastLogin = Settings.getMySQLColumnLastLogin;
        this.lastlocX = Settings.getMySQLlastlocX;
        this.lastlocY = Settings.getMySQLlastlocY;
        this.lastlocZ = Settings.getMySQLlastlocZ;
        this.lastlocWorld = Settings.getMySQLlastlocWorld;
        this.columnSalt = Settings.getMySQLColumnSalt;
        this.columnGroup = Settings.getMySQLColumnGroup;
        this.columnEmail = Settings.getMySQLColumnEmail;
        this.columnOthers = Settings.getMySQLOtherUsernameColumn;
        this.columnID = Settings.getMySQLColumnId;
        this.columnLogged = Settings.getMySQLColumnLogged;
        this.columnRealName = Settings.getMySQLColumnRealName;

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

    /**
     * Method setConnectionArguments.
     *
     * @throws RuntimeException
     */
    private synchronized void setConnectionArguments() throws RuntimeException {
        ds = new HikariDataSource();
        ds.setPoolName("AuthMeMYSQLPool");
        ds.setDriverClassName("com.mysql.jdbc.Driver");
        ds.setJdbcUrl("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?rewriteBatchedStatements=true");
        ds.setUsername(this.username);
        ds.setPassword(this.password);
        ds.setInitializationFailFast(true); // Don't start the plugin if the database is unavailable
        ds.setMaxLifetime(180000); // 3 Min
        ds.setIdleTimeout(60000); // 1 Min
        ds.setMinimumIdle(2);
        ds.setMaximumPoolSize((Runtime.getRuntime().availableProcessors() * 2) + 1);
        ConsoleLogger.info("Connection arguments loaded, Hikari ConnectionPool ready!");
    }

    /**
     * Method reloadArguments.
     *
     * @throws RuntimeException
     */
    private synchronized void reloadArguments() throws RuntimeException {
        if (ds != null) {
            ds.close();
        }
        setConnectionArguments();
        ConsoleLogger.info("Hikari ConnectionPool arguments reloaded!");
    }

    /**
     * Method getConnection.
     *
     * @return Connection * @throws SQLException
     */
    @Override
    public synchronized Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    /**
     * Method setupConnection.
     *
     * @throws SQLException
     */
    private synchronized void setupConnection() throws SQLException {
        try (Connection con = getConnection()) {
            Statement st = con.createStatement();
            DatabaseMetaData md = con.getMetaData();
            // Create table if not exists.
            String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + columnID + " INTEGER AUTO_INCREMENT,"
                + columnName + " VARCHAR(255) NOT NULL UNIQUE,"
                + columnRealName + " VARCHAR(255) NOT NULL,"
                + columnPassword + " VARCHAR(255) NOT NULL,"
                + columnIp + " VARCHAR(40) NOT NULL DEFAULT '127.0.0.1',"
                + columnLastLogin + " BIGINT NOT NULL DEFAULT '" + System.currentTimeMillis() + "',"
                + lastlocX + " DOUBLE NOT NULL DEFAULT '0.0',"
                + lastlocY + " DOUBLE NOT NULL DEFAULT '0.0',"
                + lastlocZ + " DOUBLE NOT NULL DEFAULT '0.0',"
                + lastlocWorld + " VARCHAR(255) NOT NULL DEFAULT '" + Settings.defaultWorld + "',"
                + columnEmail + " VARCHAR(255) DEFAULT 'your@email.com',"
                + columnLogged + " SMALLINT NOT NULL DEFAULT '0',"
                + "CONSTRAINT table_const_prim PRIMARY KEY (" + columnID + ")"
                + ");";
            st.executeUpdate(sql);

            ResultSet rs = md.getColumns(null, null, tableName, columnName);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + columnName + " VARCHAR(255) NOT NULL UNIQUE AFTER " + columnID + ";");
            }
            rs.close();

            rs = md.getColumns(null, null, tableName, columnRealName);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + columnRealName + " VARCHAR(255) NOT NULL AFTER " + columnName + ";");
            }
            rs.close();

            rs = md.getColumns(null, null, tableName, columnPassword);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + columnPassword + " VARCHAR(255) NOT NULL;");
            }
            rs.close();

            rs = md.getColumns(null, null, tableName, columnIp);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + columnIp + " VARCHAR(40) NOT NULL;");
            }
            rs.close();

            rs = md.getColumns(null, null, tableName, columnLastLogin);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + columnLastLogin + " BIGINT;");
            }
            rs.close();

            rs = md.getColumns(null, null, tableName, lastlocX);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + lastlocX + " DOUBLE NOT NULL DEFAULT '0.0' AFTER " + columnLastLogin + " , ADD "
                    + lastlocY + " DOUBLE NOT NULL DEFAULT '0.0' AFTER " + lastlocX + " , ADD "
                    + lastlocZ + " DOUBLE NOT NULL DEFAULT '0.0' AFTER " + lastlocY);
            }
            rs.close();

            rs = md.getColumns(null, null, tableName, lastlocX);
            if (rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " MODIFY "
                    + lastlocX + " DOUBLE NOT NULL DEFAULT '0.0', MODIFY "
                    + lastlocY + " DOUBLE NOT NULL DEFAULT '0.0', MODIFY "
                    + lastlocZ + " DOUBLE NOT NULL DEFAULT '0.0';");
            }
            rs.close();

            rs = md.getColumns(null, null, tableName, lastlocWorld);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + lastlocWorld + " VARCHAR(255) NOT NULL DEFAULT 'world' AFTER " + lastlocZ);
            }
            rs.close();

            rs = md.getColumns(null, null, tableName, columnEmail);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + columnEmail + " VARCHAR(255) DEFAULT 'your@email.com' AFTER " + lastlocWorld);
            }
            rs.close();

            rs = md.getColumns(null, null, tableName, columnLogged);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + columnLogged + " SMALLINT NOT NULL DEFAULT '0' AFTER " + columnEmail);
            }
            rs.close();

            st.close();
        }
        ConsoleLogger.info("MySQL Setup finished");
    }

    /**
     * Method isAuthAvailable.
     *
     * @param user String
     *
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#isAuthAvailable(String)
     */
    @Override
    public synchronized boolean isAuthAvailable(String user) {
        try (Connection con = getConnection()) {
        	PreparedStatement pst = con.prepareStatement(new Query()
        			.select(columnName)
        			.from(tableName)
        			.addWhere(columnName + "=?", null)
        			.build()
        			.getQuery());
            pst.setString(1, user.toLowerCase());
            ResultSet rs = pst.executeQuery();
            return rs.next();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
        return false;
    }

    /**
     * Method getAuth.
     *
     * @param user String
     *
     * @return PlayerAuth * @see fr.xephi.authme.datasource.DataSource#getAuth(String)
     */
    @Override
    public synchronized PlayerAuth getAuth(String user) {
        PlayerAuth pAuth;
        try (Connection con = getConnection()) {
        	PreparedStatement pst = con.prepareStatement(new Query()
        			.select("*")
        			.from(tableName)
        			.addWhere(columnName + "=?", null)
        			.build()
        			.getQuery());
            pst.setString(1, user.toLowerCase());
            ResultSet rs = pst.executeQuery();
            if (!rs.next()) {
                return null;
            }
            String salt = !columnSalt.isEmpty() ? rs.getString(columnSalt) : "";
            int group = !salt.isEmpty() && !columnGroup.isEmpty() ? rs.getInt(columnGroup) : -1;
            int id = rs.getInt(columnID);
            pAuth = PlayerAuth.builder()
                .name(rs.getString(columnName))
                .realName(rs.getString(columnRealName))
                .hash(rs.getString(columnPassword))
                .lastLogin(rs.getLong(columnLastLogin))
                .ip(rs.getString(columnIp))
                .locWorld(rs.getString(lastlocWorld))
                .locX(rs.getDouble(lastlocX))
                .locY(rs.getDouble(lastlocY))
                .locZ(rs.getDouble(lastlocZ))
                .email(rs.getString(columnEmail))
                .salt(salt)
                .groupId(group)
                .build();
            rs.close();
            pst.close();
            if (Settings.getPasswordHash == HashAlgorithm.XENFORO) {
                pst = con.prepareStatement(new Query()
            			.select("data")
            			.from("xf_user_authenticate")
            			.addWhere(columnID + "=?", null)
            			.build()
            			.getQuery());
                pst.setInt(1, id);
                rs = pst.executeQuery();
                if (rs.next()) {
                    Blob blob = rs.getBlob("data");
                    byte[] bytes = blob.getBytes(1, (int) blob.length());
                    pAuth.setHash(new String(bytes));
                }
            }
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
            return null;
        }
        return pAuth;
    }

    /**
     * Method saveAuth.
     *
     * @param auth PlayerAuth
     *
     * @return boolean
     *
     * @see fr.xephi.authme.datasource.DataSource#saveAuth(PlayerAuth)
     */
    @Override
    public synchronized boolean saveAuth(PlayerAuth auth) {
        try (Connection con = getConnection()) {
            PreparedStatement pst;
            PreparedStatement pst2;
            ResultSet rs;
            String sql;

            boolean useSalt = !columnSalt.isEmpty() || !auth.getSalt().isEmpty();
            sql = "INSERT INTO " + tableName + "("
                + columnName + "," + columnPassword + "," + columnIp + ","
                + columnLastLogin + "," + columnRealName
                + (useSalt ? "," + columnSalt : "")
                + ") VALUES (?,?,?,?,?" + (useSalt ? ",?" : "") + ");";
            pst = con.prepareStatement(sql);
            pst.setString(1, auth.getNickname());
            pst.setString(2, auth.getHash());
            pst.setString(3, auth.getIp());
            pst.setLong(4, auth.getLastLogin());
            pst.setString(5, auth.getRealName());
            if (useSalt) {
                pst.setString(6, auth.getSalt());
            }
            pst.executeUpdate();
            pst.close();

            if (!columnOthers.isEmpty()) {
                for (String column : columnOthers) {
                    pst = con.prepareStatement("UPDATE " + tableName + " SET " + column + "=? WHERE " + columnName + "=?;");
                    pst.setString(1, auth.getRealName());
                    pst.setString(2, auth.getNickname());
                    pst.executeUpdate();
                    pst.close();
                }
            }

            if (Settings.getPasswordHash == HashAlgorithm.PHPBB) {
                sql = "SELECT " + columnID + " FROM " + tableName + " WHERE " + columnName + "=?;";
                pst = con.prepareStatement(sql);
                pst.setString(1, auth.getNickname());
                rs = pst.executeQuery();
                if (rs.next()) {
                    int id = rs.getInt(columnID);
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
                        + ".username_clean=? WHERE " + columnName + "=?;";
                    pst2 = con.prepareStatement(sql);
                    pst2.setString(1, auth.getNickname());
                    pst2.setString(2, auth.getNickname());
                    pst2.executeUpdate();
                    pst2.close();
                    // Update player group in phpbb_users
                    sql = "UPDATE " + tableName + " SET " + tableName
                        + ".group_id=? WHERE " + columnName + "=?;";
                    pst2 = con.prepareStatement(sql);
                    pst2.setInt(1, Settings.getPhpbbGroup);
                    pst2.setString(2, auth.getNickname());
                    pst2.executeUpdate();
                    pst2.close();
                    // Get current time without ms
                    long time = System.currentTimeMillis() / 1000;
                    // Update user_regdate
                    sql = "UPDATE " + tableName + " SET " + tableName
                        + ".user_regdate=? WHERE " + columnName + "=?;";
                    pst2 = con.prepareStatement(sql);
                    pst2.setLong(1, time);
                    pst2.setString(2, auth.getNickname());
                    pst2.executeUpdate();
                    pst2.close();
                    // Update user_lastvisit
                    sql = "UPDATE " + tableName + " SET " + tableName
                        + ".user_lastvisit=? WHERE " + columnName + "=?;";
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
            } else if (Settings.getPasswordHash == HashAlgorithm.WORDPRESS) {
                pst = con.prepareStatement("SELECT " + columnID + " FROM " + tableName + " WHERE " + columnName + "=?;");
                pst.setString(1, auth.getNickname());
                rs = pst.executeQuery();
                if (rs.next()) {
                    int id = rs.getInt(columnID);
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
            } else if (Settings.getPasswordHash == HashAlgorithm.XENFORO) {
                pst = con.prepareStatement("SELECT " + columnID + " FROM " + tableName + " WHERE " + columnName + "=?;");
                pst.setString(1, auth.getNickname());
                rs = pst.executeQuery();
                if (rs.next()) {
                    int id = rs.getInt(columnID);
                    // Insert password in the correct table
                    pst2 = con.prepareStatement("INSERT INTO xf_user_authenticate (user_id, scheme_class, data) VALUES (?,?,?);");
                    pst2.setInt(1, id);
                    pst2.setString(2, "XenForo_Authentication_Core12");
                    byte[] bytes = auth.getHash().getBytes();
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
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
        return false;
    }

    /**
     * Method updatePassword.
     *
     * @param auth PlayerAuth
     *
     * @return boolean
     *
     * @see fr.xephi.authme.datasource.DataSource#updatePassword(PlayerAuth)
     */
    @Override
    public synchronized boolean updatePassword(PlayerAuth auth) {
        try (Connection con = getConnection()) {
            String sql = "UPDATE " + tableName + " SET " + columnPassword + "=? WHERE " + columnName + "=?;";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, auth.getHash());
            pst.setString(2, auth.getNickname());
            pst.executeUpdate();
            pst.close();
            if (Settings.getPasswordHash == HashAlgorithm.XENFORO) {
                sql = "SELECT " + columnID + " FROM " + tableName + " WHERE " + columnName + "=?;";
                pst = con.prepareStatement(sql);
                pst.setString(1, auth.getNickname());
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    int id = rs.getInt(columnID);
                    // Insert password in the correct table
                    sql = "UPDATE xf_user_authenticate SET data=? WHERE " + columnID + "=?;";
                    PreparedStatement pst2 = con.prepareStatement(sql);
                    byte[] bytes = auth.getHash().getBytes();
                    Blob blob = con.createBlob();
                    blob.setBytes(1, bytes);
                    pst2.setBlob(1, blob);
                    pst2.setInt(2, id);
                    pst2.executeUpdate();
                    pst2.close();
                    // ...
                    sql = "UPDATE xf_user_authenticate SET scheme_class=? WHERE " + columnID + "=?;";
                    pst2 = con.prepareStatement(sql);
                    pst2.setString(1, "XenForo_Authentication_Core12");
                    pst2.setInt(2, id);
                    pst2.executeUpdate();
                    pst2.close();
                }
                rs.close();
                pst.close();
            }
            return true;
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
        return false;
    }

    /**
     * Method updateSession.
     *
     * @param auth PlayerAuth
     *
     * @return boolean
     *
     * @see fr.xephi.authme.datasource.DataSource#updateSession(PlayerAuth)
     */
    @Override
    public synchronized boolean updateSession(PlayerAuth auth) {
        try(Connection con = getConnection())  {
        	PreparedStatement pst = con.prepareStatement(new Query()
        			.update()
        			.from(tableName)
        			.addUpdateSet(columnIp + "=?")
        			.addUpdateSet(columnLastLogin + "=?")
        			.addUpdateSet(columnRealName + "=?")
        			.addWhere(columnName + "=?", null)
        			.build()
        			.getQuery());
            pst.setString(1, auth.getIp());
            pst.setLong(2, auth.getLastLogin());
            pst.setString(3, auth.getRealName());
            pst.setString(4, auth.getNickname());
            pst.executeUpdate();
            pst.close();
            return true;
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
        }
        return false;
    }

    /**
     * Method purgeDatabase.
     *
     * @param until long
     *
     * @return int
     *
     * @see fr.xephi.authme.datasource.DataSource#purgeDatabase(long)
     */
    @Override
    public synchronized int purgeDatabase(long until) {
        int result = 0;
        try(Connection con = getConnection())  {
        	PreparedStatement pst = con.prepareStatement(new Query()
        			.delete()
        			.from(tableName)
        			.addWhere(columnLastLogin + "<?", null)
        			.build()
        			.getQuery());
            pst.setLong(1, until);
            result = pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
        return result;
    }

    /**
     * Method autoPurgeDatabase.
     *
     * @param until long
     *
     * @return List
     *
     * @see fr.xephi.authme.datasource.DataSource#autoPurgeDatabase(long)
     */
    @Override
    public synchronized List<String> autoPurgeDatabase(long until) {
        List<String> list = new ArrayList<>();
        try(Connection con = getConnection())  {
        	PreparedStatement st = con.prepareStatement(new Query()
        			.select(columnName)
        			.from(tableName)
        			.addWhere(columnLastLogin + "<" + until, null)
        			.build()
        			.getQuery());
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                list.add(rs.getString(columnName));
            }
            rs.close();
            st.close();
            st = con.prepareStatement(new Query()
            		.delete()
            		.from(tableName)
            		.addWhere(columnLastLogin + "<" + until, null)
            		.build()
            		.getQuery());
            st.executeUpdate();
            st.close();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
        return list;
    }

    /**
     * Method removeAuth.
     *
     * @param user String
     *
     * @return boolean
     *
     * @see fr.xephi.authme.datasource.DataSource#removeAuth(String)
     */
    @Override
    public synchronized boolean removeAuth(String user) {
        user = user.toLowerCase();
        try (Connection con = getConnection()) {
            String sql;
            PreparedStatement pst;
            if (Settings.getPasswordHash == HashAlgorithm.XENFORO) {
                sql = "SELECT " + columnID + " FROM " + tableName + " WHERE " + columnName + "=?;";
                pst = con.prepareStatement(sql);
                pst.setString(1, user);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    int id = rs.getInt(columnID);
                    sql = "DELETE FROM xf_user_authenticate WHERE " + columnID + "=" + id;
                    Statement st = con.createStatement();
                    st.executeUpdate(sql);
                    st.close();
                }
                rs.close();
                pst.close();
            }
            pst = con.prepareStatement("DELETE FROM " + tableName + " WHERE " + columnName + "=?;");
            pst.setString(1, user);
            pst.executeUpdate();
            return true;
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
        return false;
    }

    /**
     * Method updateQuitLoc.
     *
     * @param auth PlayerAuth
     *
     * @return boolean
     *
     * @see fr.xephi.authme.datasource.DataSource#updateQuitLoc(PlayerAuth)
     */
    @Override
    public synchronized boolean updateQuitLoc(PlayerAuth auth) {
        try(Connection con = getConnection())  {
        	PreparedStatement pst = con.prepareStatement(new Query()
        			.update()
        			.from(tableName)
        			.addUpdateSet(lastlocX + "=?")
        			.addUpdateSet(lastlocY + "=?")
        			.addUpdateSet(lastlocZ + "=?")
        			.addUpdateSet(lastlocWorld + "=?")
        			.addWhere(columnName + "=?", null)
        			.build()
        			.getQuery());
            pst.setDouble(1, auth.getQuitLocX());
            pst.setDouble(2, auth.getQuitLocY());
            pst.setDouble(3, auth.getQuitLocZ());
            pst.setString(4, auth.getWorld());
            pst.setString(5, auth.getNickname());
            pst.executeUpdate();
            pst.close();
            return true;
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
        return false;
    }

    /**
     * Method getIps.
     *
     * @param ip String
     *
     * @return int
     *
     * @see fr.xephi.authme.datasource.DataSource#getIps(String)
     */
    @Override
    public synchronized int getIps(String ip) {
        int countIp = 0;
        try (Connection con = getConnection()) {
        	PreparedStatement pst = con.prepareStatement(new Query()
        			.select("COUNT(*)")
        			.from(tableName)
        			.addWhere(columnIp + "=?", null)
        			.build()
        			.getQuery());
            pst.setString(1, ip);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                countIp = rs.getInt(1);
            }
            rs.close();
            pst.close();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
        return countIp;
    }

    /**
     * Method updateEmail.
     *
     * @param auth PlayerAuth
     *
     * @return boolean
     *
     * @see fr.xephi.authme.datasource.DataSource#updateEmail(PlayerAuth)
     */
    @Override
    public synchronized boolean updateEmail(PlayerAuth auth) {
        try (Connection con = getConnection()) {
            PreparedStatement pst = con.prepareStatement(new Query()
            		.update()
            		.from(tableName)
            		.addUpdateSet(columnEmail + "=?")
            		.addWhere(columnName + "=?", null)
            		.build()
            		.getQuery());
            pst.setString(1, auth.getEmail());
            pst.setString(2, auth.getNickname());
            pst.executeUpdate();
            pst.close();
            return true;
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
        return false;
    }

    /**
     * Method updateSalt.
     *
     * @param auth PlayerAuth
     *
     * @return boolean
     *
     * @see fr.xephi.authme.datasource.DataSource#updateSalt(PlayerAuth)
     */
    @Override
    public synchronized boolean updateSalt(PlayerAuth auth) {
        if (columnSalt.isEmpty()) {
            return false;
        }
        try (Connection con = getConnection()) {
            PreparedStatement pst = con.prepareStatement(new Query()
            		.update()
            		.from(tableName)
            		.addUpdateSet(columnSalt + "=?")
            		.addWhere(columnName + "=?", null)
            		.build()
            		.getQuery());
            pst.setString(1, auth.getSalt());
            pst.setString(2, auth.getNickname());
            pst.executeUpdate();
            pst.close();
            return true;
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
        return false;
    }

    /**
     * Method reload.
     *
     * @see fr.xephi.authme.datasource.DataSource#reload()
     */
    @Override
    public void reload() {
        try {
            reloadArguments();
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.showError("Can't reconnect to MySQL database... Please check your MySQL configuration!");
            ConsoleLogger.writeStackTrace(ex);
            AuthMe.getInstance().stopOrUnload();
        }
    }

    /**
     * Method close.
     *
     * @see fr.xephi.authme.datasource.DataSource#close()
     */
    @Override
    public synchronized void close() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
    }

    /**
     * Method getAllAuthsByName.
     *
     * @param auth PlayerAuth
     *
     * @return List
     *
     * @see fr.xephi.authme.datasource.DataSource#getAllAuthsByName(PlayerAuth)
     */
    @Override
    public synchronized List<String> getAllAuthsByName(PlayerAuth auth) {
        List<String> result = new ArrayList<>();
        try (Connection con = getConnection()) {
            PreparedStatement pst = getConnection().prepareStatement(new Query()
            		.select(columnName)
            		.from(tableName)
            		.addWhere(columnIp + "=?", null)
            		.build()
            		.getQuery());
            pst.setString(1, auth.getIp());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                result.add(rs.getString(columnName));
            }
            rs.close();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
        return result;
    }

    /**
     * Method getAllAuthsByIp.
     *
     * @param ip String
     *
     * @return List
     *
     * @see fr.xephi.authme.datasource.DataSource#getAllAuthsByIp(String)
     */
    @Override
    public synchronized List<String> getAllAuthsByIp(String ip) {
        List<String> result = new ArrayList<>();
        try {
            PreparedStatement pst = getConnection().prepareStatement(new Query()
            		.select(columnName)
            		.from(tableName)
            		.addWhere(columnIp + "=?", null)
            		.build()
            		.getQuery());
            pst.setString(1, ip);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                result.add(rs.getString(columnName));
            }
            rs.close();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
        return result;
    }

    /**
     * Method getAllAuthsByEmail.
     *
     * @param email String
     *
     * @return List
     *
     * @see fr.xephi.authme.datasource.DataSource#getAllAuthsByEmail(String)
     */
    @Override
    public synchronized List<String> getAllAuthsByEmail(String email){
        List<String> countEmail = new ArrayList<>();
        try {
            PreparedStatement pst = getConnection().prepareStatement(new Query()
            		.select(columnName)
            		.from(tableName)
            		.addWhere(columnEmail + "=?", null)
            		.build()
            		.getQuery());
            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                countEmail.add(rs.getString(columnName));
            }
            rs.close();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
        return countEmail;
    }

    /**
     * Method purgeBanned.
     *
     * @param banned List<String>
     *
     * @see fr.xephi.authme.datasource.DataSource#purgeBanned(List)
     */
    @Override
    public synchronized void purgeBanned(List<String> banned) {
        try (Connection con = getConnection()) {
        	PreparedStatement pst = con.prepareStatement(new Query()
        			.delete()
        			.from(tableName)
        			.addWhere(columnName + "=?", null)
        			.build()
        			.getQuery());
            for (String name : banned) {
                pst.setString(1, name);
                pst.executeUpdate();
            }
            pst.close();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
    }

    /**
     * Method getType.
     *
     * @return DataSourceType * @see fr.xephi.authme.datasource.DataSource#getType()
     */
    @Override
    public DataSourceType getType() {
        return DataSourceType.MYSQL;
    }

    /**
     * Method isLogged.
     *
     * @param user String
     *
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#isLogged(String)
     */
    @Override
    public boolean isLogged(String user) {
        boolean isLogged = false;
        try {
        	PreparedStatement pst = getConnection().prepareStatement(new Query()
        			.select(columnLogged)
        			.from(tableName)
        			.addWhere(columnName + "=?", null)
        			.build()
        			.getQuery());
        	pst.setString(1, user);
            ResultSet rs = pst.executeQuery();
            isLogged = rs.next() && (rs.getInt(columnLogged) == 1);
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
        return isLogged;
    }

    /**
     * Method setLogged.
     *
     * @param user String
     *
     * @see fr.xephi.authme.datasource.DataSource#setLogged(String)
     */
    @Override
    public void setLogged(String user) {
        try {
            PreparedStatement pst = getConnection().prepareStatement(new Query()
            		.update()
            		.from(tableName)
            		.addUpdateSet(columnLogged + "='1'")
            		.addWhere(columnName + "=?", null)
            		.build()
            		.getQuery());
            pst.setString(1, user.toLowerCase());
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
    }

    /**
     * Method setUnlogged.
     *
     * @param user String
     *
     * @see fr.xephi.authme.datasource.DataSource#setUnlogged(String)
     */
    @Override
    public void setUnlogged(String user) {
        try {
            PreparedStatement pst = getConnection().prepareStatement(new Query()
            		.update()
            		.from(tableName)
            		.addUpdateSet(columnLogged + "='0'")
            		.addWhere(columnName + "=?", null)
            		.build()
            		.getQuery());
            pst.setString(1, user.toLowerCase());
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
    }

    /**
     * Method purgeLogged.
     *
     * @see fr.xephi.authme.datasource.DataSource#purgeLogged()
     */
    @Override
    public void purgeLogged() {
        try (Connection con = getConnection()) {
            PreparedStatement pst = con.prepareStatement(new Query()
            		.update()
            		.from(tableName)
            		.addUpdateSet(columnLogged + "=" + 0)
            		.addWhere(columnLogged + "=" + 1, null)
            		.build()
            		.getQuery());
            pst.executeUpdate();
            pst.close();
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
    }

    /**
     * Method getAccountsRegistered.
     *
     * @return int
     *
     * @see fr.xephi.authme.datasource.DataSource#getAccountsRegistered()
     */
    @Override
    public int getAccountsRegistered() {
        int result = 0;
        try (Connection con = getConnection()) {
        	PreparedStatement st = con.prepareStatement(new Query()
        			.select("COUNT(*)")
        			.from(tableName)
        			.build()
        			.getQuery());
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                result = rs.getInt(1);
            }
            rs.close();
            st.close();
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
        return result;
    }

    /**
     * Method updateName.
     *
     * @param oldOne String
     * @param newOne String
     *
     * @see fr.xephi.authme.datasource.DataSource#updateName(String, String)
     */
    @Override
    public void updateName(String oldOne, String newOne) {
        try (Connection con = getConnection()) {
            PreparedStatement pst =
            		con.prepareStatement(new Query()
            		.update()
            		.from(tableName)
            		.addUpdateSet(columnName + "=?")
            		.addWhere(columnName + "=?", null)
            		.build()
            		.getQuery());
            pst.setString(1, newOne);
            pst.setString(2, oldOne);
            pst.executeUpdate();
            pst.close();
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
    }

    /**
     * Method getAllAuths.
     *
     * @return List
     *
     * @see fr.xephi.authme.datasource.DataSource#getAllAuths()
     */
    @Override
    public List<PlayerAuth> getAllAuths() {
        List<PlayerAuth> auths = new ArrayList<>();
        try (Connection con = getConnection()) {
        	PreparedStatement st = con.prepareStatement(new Query()
            		.select("*")
            		.from(tableName)
            		.build()
            		.getQuery());
            ResultSet rs = st
            		.executeQuery();
            PreparedStatement pst = con.prepareStatement(new Query()
            		.select("data")
            		.from("xf_user_authenticate")
            		.addWhere(columnID + "=?", null)
            		.build()
            		.getQuery());
            while (rs.next()) {
                String salt = !columnSalt.isEmpty() ? rs.getString(columnSalt) : "";
                int group = !salt.isEmpty() && !columnGroup.isEmpty() ? rs.getInt(columnGroup) : -1;
                PlayerAuth pAuth = PlayerAuth.builder()
                    .name(rs.getString(columnName))
                    .realName(rs.getString(columnRealName))
                    .hash(rs.getString(columnPassword))
                    .lastLogin(rs.getLong(columnLastLogin))
                    .ip(rs.getString(columnIp))
                    .locWorld(rs.getString(lastlocWorld))
                    .locX(rs.getDouble(lastlocX))
                    .locY(rs.getDouble(lastlocY))
                    .locZ(rs.getDouble(lastlocZ))
                    .email(rs.getString(columnEmail))
                    .salt(salt)
                    .groupId(group)
                    .build();

                if (Settings.getPasswordHash == HashAlgorithm.XENFORO) {
                    int id = rs.getInt(columnID);
                    pst.setInt(1, id);
                    ResultSet rs2 = pst.executeQuery();
                    if (rs2.next()) {
                        Blob blob = rs2.getBlob("data");
                        byte[] bytes = blob.getBytes(1, (int) blob.length());
                        pAuth.setHash(new String(bytes));
                    }
                    rs2.close();
                }
                auths.add(pAuth);
            }
            pst.close();
            rs.close();
            st.close();
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
        return auths;
    }

    /**
     * Method getLoggedPlayers.
     *
     * @return List
     *
     * @see fr.xephi.authme.datasource.DataSource#getLoggedPlayers()
     */
    @Override
    public List<PlayerAuth> getLoggedPlayers() {
        List<PlayerAuth> auths = new ArrayList<>();
        try (Connection con = getConnection()) {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM " + tableName + " WHERE " + columnLogged + "=1;");
            PreparedStatement pst = con.prepareStatement("SELECT data FROM xf_user_authenticate WHERE " + columnID + "=?;");
            while (rs.next()) {
                String salt = !columnSalt.isEmpty() ? rs.getString(columnSalt) : "";
                int group = !salt.isEmpty() && !columnGroup.isEmpty() ? rs.getInt(columnGroup) : -1;
                PlayerAuth pAuth = PlayerAuth.builder()
                    .name(rs.getString(columnName))
                    .realName(rs.getString(columnRealName))
                    .hash(rs.getString(columnPassword))
                    .lastLogin(rs.getLong(columnLastLogin))
                    .ip(rs.getString(columnIp))
                    .locWorld(rs.getString(lastlocWorld))
                    .locX(rs.getDouble(lastlocX))
                    .locY(rs.getDouble(lastlocY))
                    .locZ(rs.getDouble(lastlocZ))
                    .email(rs.getString(columnEmail))
                    .salt(salt)
                    .groupId(group)
                    .build();

                if (Settings.getPasswordHash == HashAlgorithm.XENFORO) {
                    int id = rs.getInt(columnID);
                    pst.setInt(1, id);
                    ResultSet rs2 = pst.executeQuery();
                    if (rs2.next()) {
                        Blob blob = rs2.getBlob("data");
                        byte[] bytes = blob.getBytes(1, (int) blob.length());
                        pAuth.setHash(new String(bytes));
                    }
                    rs2.close();
                }
                auths.add(pAuth);
            }
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
        return auths;
    }

}
