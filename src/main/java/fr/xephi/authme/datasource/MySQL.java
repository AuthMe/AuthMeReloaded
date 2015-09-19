package fr.xephi.authme.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.PoolInitializationException;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.settings.Settings;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySQL implements DataSource {

    private String host;
    private String port;
    private String username;
    private String password;
    private String database;
    private String tableName;
    private String columnName;
    private String columnPassword;
    private String columnIp;
    private String columnLastLogin;
    private String columnSalt;
    private String columnGroup;
    private String lastlocX;
    private String lastlocY;
    private String lastlocZ;
    private String lastlocWorld;
    private String columnEmail;
    private String columnID;
    private String columnLogged;
    private List<String> columnOthers;
    private HikariDataSource ds;
    private String columnRealName;
    private int maxConnections;

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
        this.maxConnections = Settings.getMySQLMaxConnections;

        // Set the connection arguments (and check if connection is ok)
        try {
            this.setConnectionArguments();
        } catch (RuntimeException e) {
            if (e instanceof IllegalArgumentException) {
                ConsoleLogger.showError("Invalid database arguments! Please check your configuration!");
                ConsoleLogger.showError("If this error persists, please report it to the developer! SHUTDOWN...");
                throw new IllegalArgumentException(e);
            }
            if (e instanceof PoolInitializationException) {
                ConsoleLogger.showError("Can't initialize database connection! Please check your configuration!");
                ConsoleLogger.showError("If this error persists, please report it to the developer! SHUTDOWN...");
                throw new PoolInitializationException(e);
            }
            ConsoleLogger.showError("Can't use the Hikari Connection Pool! Please, report this error to the developer! SHUTDOWN...");
            throw e;
        }

        // Initialize the database
        try {
            this.setupConnection();
        } catch (SQLException e) {
            this.close();
            ConsoleLogger.showError("Can't initialize the MySQL database... Please check your database settings in the config.yml file! SHUTDOWN...");
            ConsoleLogger.showError("If this error persists, please report it to the developer! SHUTDOWN...");
            throw e;
        }
    }

    private synchronized void setConnectionArguments()
            throws ClassNotFoundException, IllegalArgumentException {
        HikariConfig config = new HikariConfig();
        config.setPoolName("AuthMeMYSQLPool");
        config.setDriverClassName("com.mysql.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database);
        config.setUsername(this.username);
        config.setPassword(this.password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("autoReconnect", false);
        config.setInitializationFailFast(true); // Don't start the plugin if the database is unavariable
        config.setMaxLifetime(180000); // 3 Min
        config.setIdleTimeout(60000); // 1 Min
        config.setMaximumPoolSize(maxConnections);
        ds = new HikariDataSource(config);
        ConsoleLogger.info("Connection arguments loaded, Hikari ConnectionPool ready!");
    }

    private synchronized void reloadArguments()
            throws ClassNotFoundException, IllegalArgumentException {
        if (ds != null) {
            ds.close();
        }
        setConnectionArguments();
        ConsoleLogger.info("Hikari ConnectionPool arguments reloaded!");
    }

    private synchronized Connection getConnection() throws SQLException {
        Connection con;
        con = ds.getConnection();
        return con;
    }

    private synchronized void setupConnection() throws SQLException {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            con = getConnection();
            st = con.createStatement();
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + tableName + " (" + columnID + " INTEGER AUTO_INCREMENT," + columnName + " VARCHAR(255) NOT NULL UNIQUE," + columnPassword + " VARCHAR(255) NOT NULL," + columnIp + " VARCHAR(40) NOT NULL DEFAULT '127.0.0.1'," + columnLastLogin + " BIGINT NOT NULL DEFAULT '" + System.currentTimeMillis() + "'," + lastlocX + " DOUBLE NOT NULL DEFAULT '0.0'," + lastlocY + " DOUBLE NOT NULL DEFAULT '0.0'," + lastlocZ + " DOUBLE NOT NULL DEFAULT '0.0'," + lastlocWorld + " VARCHAR(255) NOT NULL DEFAULT '" + Settings.defaultWorld + "'," + columnEmail + " VARCHAR(255) DEFAULT 'your@email.com'," + columnLogged + " SMALLINT NOT NULL DEFAULT '0'," + "CONSTRAINT table_const_prim PRIMARY KEY (" + columnID + "));");
            rs = con.getMetaData().getColumns(null, null, tableName, columnPassword);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnPassword + " VARCHAR(255) NOT NULL;");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, columnIp);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnIp + " VARCHAR(40) NOT NULL;");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, columnLastLogin);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnLastLogin + " BIGINT;");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, lastlocX);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + lastlocX + " DOUBLE NOT NULL DEFAULT '0.0' AFTER " + columnLastLogin + " , ADD " + lastlocY + " DOUBLE NOT NULL DEFAULT '0.0' AFTER " + lastlocX + " , ADD " + lastlocZ + " DOUBLE NOT NULL DEFAULT '0.0' AFTER " + lastlocY + ";");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, lastlocWorld);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + lastlocWorld + " VARCHAR(255) NOT NULL DEFAULT 'world' AFTER " + lastlocZ + ";");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, columnEmail);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnEmail + " VARCHAR(255) DEFAULT 'your@email.com' AFTER " + lastlocWorld + ";");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, columnLogged);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnLogged + " SMALLINT NOT NULL DEFAULT '0' AFTER " + columnEmail + ";");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, lastlocX);
            if (rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " MODIFY " + lastlocX + " DOUBLE NOT NULL DEFAULT '0.0', MODIFY " + lastlocY + " DOUBLE NOT NULL DEFAULT '0.0', MODIFY " + lastlocZ + " DOUBLE NOT NULL DEFAULT '0.0';");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, columnRealName);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnRealName + " VARCHAR(255) NOT NULL DEFAULT 'Player' AFTER " + columnLogged + ";");
            }
        } finally {
            close(rs);
            close(st);
            close(con);
        }
        ConsoleLogger.info("MySQL Setup finished");
    }

    @Override
    public synchronized boolean isAuthAvailable(String user) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            con = getConnection();
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE LOWER(" + columnName + ")=LOWER(?);");
            pst.setString(1, user);
            rs = pst.executeQuery();
            return rs.next();
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(rs);
            close(pst);
            close(con);
        }
    }

    @Override
    public synchronized PlayerAuth getAuth(String user) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        PlayerAuth pAuth = null;
        int id;
        try {
            con = getConnection();
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE LOWER(" + columnName + ")=LOWER(?);");
            pst.setString(1, user);
            rs = pst.executeQuery();
            if (rs.next()) {
                id = rs.getInt(columnID);
                if (rs.getString(columnIp).isEmpty() && rs.getString(columnIp) != null) {
                    pAuth = new PlayerAuth(rs.getString(columnName).toLowerCase(), rs.getString(columnPassword), "192.168.0.1", rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail), rs.getString(columnRealName));
                } else {
                    if (!columnSalt.isEmpty()) {
                        if (!columnGroup.isEmpty())
                            pAuth = new PlayerAuth(rs.getString(columnName).toLowerCase(), rs.getString(columnPassword), rs.getString(columnSalt), rs.getInt(columnGroup), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail), rs.getString(columnRealName));
                        else
                            pAuth = new PlayerAuth(rs.getString(columnName).toLowerCase(), rs.getString(columnPassword), rs.getString(columnSalt), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail), rs.getString(columnRealName));
                    } else {
                        pAuth = new PlayerAuth(rs.getString(columnName).toLowerCase(), rs.getString(columnPassword), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail), rs.getString(columnRealName));
                    }
                }
                if (Settings.getPasswordHash == HashAlgorithm.XENFORO) {
                    rs.close();
                    pst.close();
                    pst = con.prepareStatement("SELECT * FROM xf_user_authenticate WHERE " + columnID + "=?;");
                    pst.setInt(1, id);
                    rs = pst.executeQuery();
                    if (rs.next()) {
                        Blob blob = rs.getBlob("data");
                        byte[] bytes = blob.getBytes(1, (int) blob.length());
                        pAuth.setHash(new String(bytes));
                    }
                }
            } else {
                return null;
            }
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            return null;
        } finally {
            close(rs);
            close(pst);
            close(con);
        }
        return pAuth;
    }

    @Override
    public synchronized boolean saveAuth(PlayerAuth auth) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            con = getConnection();
            if ((columnSalt == null || columnSalt.isEmpty()) || (auth.getSalt() == null || auth.getSalt().isEmpty())) {
                pst = con.prepareStatement("INSERT INTO " + tableName + "(" + columnName + "," + columnPassword + "," + columnIp + "," + columnLastLogin + "," + columnRealName + ") VALUES (?,?,?,?,?);");
                pst.setString(1, auth.getNickname());
                pst.setString(2, auth.getHash());
                pst.setString(3, auth.getIp());
                pst.setLong(4, auth.getLastLogin());
                pst.setString(5, auth.getRealName());
                pst.executeUpdate();
                pst.close();
            } else {
                pst = con.prepareStatement("INSERT INTO " + tableName + "(" + columnName + "," + columnPassword + "," + columnIp + "," + columnLastLogin + "," + columnSalt + "," + columnRealName + ") VALUES (?,?,?,?,?,?);");
                pst.setString(1, auth.getNickname());
                pst.setString(2, auth.getHash());
                pst.setString(3, auth.getIp());
                pst.setLong(4, auth.getLastLogin());
                pst.setString(5, auth.getSalt());
                pst.setString(6, auth.getRealName());
                pst.executeUpdate();
                pst.close();
            }
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
                PreparedStatement pst2 = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnName + "=?;");
                pst2.setString(1, auth.getNickname());
                rs = pst2.executeQuery();
                if (rs.next()) {
                    int id = rs.getInt(columnID);
                    // Insert player in phpbb_user_group
                    pst = con.prepareStatement("INSERT INTO " + Settings.getPhpbbPrefix + "user_group (group_id, user_id, group_leader, user_pending) VALUES (?,?,?,?);");
                    pst.setInt(1, Settings.getPhpbbGroup);
                    pst.setInt(2, id);
                    pst.setInt(3, 0);
                    pst.setInt(4, 0);
                    pst.executeUpdate();
                    pst.close();
                    // Update username_clean in phpbb_users
                    pst = con.prepareStatement("UPDATE " + tableName + " SET " + tableName + ".username_clean=? WHERE " + columnName + "=?;");
                    pst.setString(1, auth.getNickname().toLowerCase());
                    pst.setString(2, auth.getNickname());
                    pst.executeUpdate();
                    pst.close();
                    // Update player group in phpbb_users
                    pst = con.prepareStatement("UPDATE " + tableName + " SET " + tableName + ".group_id=? WHERE " + columnName + "=?;");
                    pst.setInt(1, Settings.getPhpbbGroup);
                    pst.setString(2, auth.getNickname());
                    pst.executeUpdate();
                    pst.close();
                    // Get current time without ms
                    long time = System.currentTimeMillis() / 1000;
                    // Update user_regdate
                    pst = con.prepareStatement("UPDATE " + tableName + " SET " + tableName + ".user_regdate=? WHERE " + columnName + "=?;");
                    pst.setLong(1, time);
                    pst.setString(2, auth.getNickname());
                    pst.executeUpdate();
                    pst.close();
                    // Update user_lastvisit
                    pst = con.prepareStatement("UPDATE " + tableName + " SET " + tableName + ".user_lastvisit=? WHERE " + columnName + "=?;");
                    pst.setLong(1, time);
                    pst.setString(2, auth.getNickname());
                    pst.executeUpdate();
                    pst.close();
                    // Increment num_users
                    pst = con.prepareStatement("UPDATE " + Settings.getPhpbbPrefix + "config SET config_value = config_value + 1 WHERE config_name = 'num_users';");
                    pst.executeUpdate();
                    pst.close();
                }
                rs.close();
                pst2.close();
            }
            if (Settings.getPasswordHash == HashAlgorithm.WORDPRESS) {
                pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnName + "=?;");
                pst.setString(1, auth.getNickname());
                rs = pst.executeQuery();
                if (rs.next()) {
                    int id = rs.getInt(columnID);
                    // First Name
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "first_name");
                    pst.setString(3, "");
                    pst.executeUpdate();
                    pst.close();
                    // Last Name
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "last_name");
                    pst.setString(3, "");
                    pst.executeUpdate();
                    pst.close();
                    // Nick Name
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "nickname");
                    pst.setString(3, auth.getNickname());
                    pst.executeUpdate();
                    pst.close();
                    // Description
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "description");
                    pst.setString(3, "");
                    pst.executeUpdate();
                    pst.close();
                    // Rich_Editing
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "rich_editing");
                    pst.setString(3, "true");
                    pst.executeUpdate();
                    pst.close();
                    // Comments_Shortcuts
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "comment_shortcuts");
                    pst.setString(3, "false");
                    pst.executeUpdate();
                    pst.close();
                    // admin_color
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "admin_color");
                    pst.setString(3, "fresh");
                    pst.executeUpdate();
                    pst.close();
                    // use_ssl
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "use_ssl");
                    pst.setString(3, "0");
                    pst.executeUpdate();
                    pst.close();
                    // show_admin_bar_front
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "show_admin_bar_front");
                    pst.setString(3, "true");
                    pst.executeUpdate();
                    pst.close();
                    // wp_capabilities
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "wp_capabilities");
                    pst.setString(3, "a:1:{s:10:\"subscriber\";b:1;}");
                    pst.executeUpdate();
                    pst.close();
                    // wp_user_level
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "wp_user_level");
                    pst.setString(3, "0");
                    pst.executeUpdate();
                    pst.close();
                    // default_password_nag
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "default_password_nag");
                    pst.setString(3, "");
                    pst.executeUpdate();
                    pst.close();
                }
                rs.close();
            }
            if (Settings.getPasswordHash == HashAlgorithm.XENFORO) {
                pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnName + "=?;");
                pst.setString(1, auth.getNickname());
                rs = pst.executeQuery();
                if (rs.next()) {
                    int id = rs.getInt(columnID);
                    // Insert password in the correct table
                    pst = con.prepareStatement("INSERT INTO xf_user_authenticate (user_id, scheme_class, data) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "XenForo_Authentication_Core12");
                    byte[] bytes = auth.getHash().getBytes();
                    Blob blob = con.createBlob();
                    blob.setBytes(1, bytes);
                    pst.setBlob(3, blob);
                    pst.executeUpdate();
                }
                rs.close();
            }
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(rs);
            close(pst);
            close(con);
        }
        return true;
    }

    @Override
    public synchronized boolean updatePassword(PlayerAuth auth) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            con = getConnection();
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnPassword + "=? WHERE LOWER(" + columnName + ")=?;");
            pst.setString(1, auth.getHash());
            pst.setString(2, auth.getNickname());
            pst.executeUpdate();
            pst.close();
            if (Settings.getPasswordHash == HashAlgorithm.XENFORO) {
                pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE LOWER(" + columnName + ")=?;");
                pst.setString(1, auth.getNickname());
                rs = pst.executeQuery();
                if (rs.next()) {
                    int id = rs.getInt(columnID);
                    // Insert password in the correct table
                    pst = con.prepareStatement("UPDATE xf_user_authenticate SET data=? WHERE " + columnID + "=?;");
                    byte[] bytes = auth.getHash().getBytes();
                    Blob blob = con.createBlob();
                    blob.setBytes(1, bytes);
                    pst.setBlob(1, blob);
                    pst.setInt(2, id);
                    pst.executeUpdate();
                    pst = con.prepareStatement("UPDATE xf_user_authenticate SET scheme_class=? WHERE " + columnID + "=?;");
                    pst.setString(1, "XenForo_Authentication_Core12");
                    pst.setInt(2, id);
                    pst.executeUpdate();
                }
                rs.close();
            }
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(rs);
            close(pst);
            close(con);
        }
        return true;
    }

    @Override
    public synchronized boolean updateSession(PlayerAuth auth) {
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = getConnection();
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnIp + "=?, " + columnLastLogin + "=?, " + columnRealName + "=? WHERE LOWER(" + columnName + ")=?;");
            pst.setString(1, auth.getIp());
            pst.setLong(2, auth.getLastLogin());
            pst.setString(3, auth.getRealName());
            pst.setString(4, auth.getNickname());
            pst.executeUpdate();
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(pst);
            close(con);
        }
        return true;
    }

    @Override
    public synchronized int purgeDatabase(long until) {
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = getConnection();
            pst = con.prepareStatement("DELETE FROM " + tableName + " WHERE " + columnLastLogin + "<?;");
            pst.setLong(1, until);
            return pst.executeUpdate();
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            return 0;
        } finally {
            close(pst);
            close(con);
        }
    }

    @Override
    public synchronized List<String> autoPurgeDatabase(long until) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        List<String> list = new ArrayList<>();
        try {
            con = getConnection();
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnLastLogin + "<?;");
            pst.setLong(1, until);
            rs = pst.executeQuery();
            while (rs.next()) {
                list.add(rs.getString(columnName));
            }
            pst.close();
            pst = con.prepareStatement("DELETE FROM " + tableName + " WHERE " + columnLastLogin + "<?;");
            pst.setLong(1, until);
            pst.executeUpdate();
            return list;
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<>();
        } finally {
            close(rs);
            close(pst);
            close(con);
        }
    }

    @Override
    public synchronized boolean removeAuth(String user) {
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = getConnection();
            if (Settings.getPasswordHash == HashAlgorithm.XENFORO) {
                int id;
                ResultSet rs;
                pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE LOWER(" + columnName + ")=?;");
                pst.setString(1, user);
                rs = pst.executeQuery();
                if (rs.next()) {
                    id = rs.getInt(columnID);
                    // Remove data
                    PreparedStatement pst2 = con.prepareStatement("DELETE FROM xf_user_authenticate WHERE " + columnID + "=?;");
                    pst2.setInt(1, id);
                    pst2.executeUpdate();
                    pst2.close();
                }
            }
            if (pst != null && !pst.isClosed())
                pst.close();
            pst = con.prepareStatement("DELETE FROM " + tableName + " WHERE LOWER(" + columnName + ")=?;");
            pst.setString(1, user);
            pst.executeUpdate();
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(pst);
            close(con);
        }
        return true;
    }

    @Override
    public synchronized boolean updateQuitLoc(PlayerAuth auth) {
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = getConnection();
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + lastlocX + " =?, " + lastlocY + "=?, " + lastlocZ + "=?, " + lastlocWorld + "=? WHERE LOWER(" + columnName + ")=?;");
            pst.setDouble(1, auth.getQuitLocX());
            pst.setDouble(2, auth.getQuitLocY());
            pst.setDouble(3, auth.getQuitLocZ());
            pst.setString(4, auth.getWorld());
            pst.setString(5, auth.getNickname());
            pst.executeUpdate();
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(pst);
            close(con);
        }
        return true;
    }

    @Override
    public synchronized int getIps(String ip) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        int countIp = 0;
        try {
            con = getConnection();
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnIp + "=?;");
            pst.setString(1, ip);
            rs = pst.executeQuery();
            while (rs.next()) {
                countIp++;
            }
            return countIp;
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            return 0;
        } finally {
            close(rs);
            close(pst);
            close(con);
        }
    }

    @Override
    public synchronized boolean updateEmail(PlayerAuth auth) {
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = getConnection();
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnEmail + " =? WHERE LOWER(" + columnName + ")=?;");
            pst.setString(1, auth.getEmail());
            pst.setString(2, auth.getNickname());
            pst.executeUpdate();
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(pst);
            close(con);
        }
        return true;
    }

    @Override
    public synchronized boolean updateSalt(PlayerAuth auth) {
        if (columnSalt.isEmpty()) {
            return false;
        }
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = getConnection();
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnSalt + " =? WHERE LOWER(" + columnName + ")=?;");
            pst.setString(1, auth.getSalt());
            pst.setString(2, auth.getNickname());
            pst.executeUpdate();
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(pst);
            close(con);
        }
        return true;
    }

    @Override
    public void reload() {
        try {
            reloadArguments();
        } catch (Exception e) {
            ConsoleLogger.showError(e.getMessage());
            ConsoleLogger.showError("Can't reconnect to MySQL database... Please check your MySQL informations ! SHUTDOWN...");
            if (Settings.isStopEnabled) {
                AuthMe.getInstance().getServer().shutdown();
            }
            if (!Settings.isStopEnabled)
                AuthMe.getInstance().getServer().getPluginManager().disablePlugin(AuthMe.getInstance());
        }
    }

    @Override
    public synchronized void close() {
        if (ds != null)
            ds.close();
    }

    private void close(AutoCloseable o) {
        if (o != null) {
            try {
                o.close();
            } catch (Exception ex) {
                ConsoleLogger.showError(ex.getMessage());
            }
        }
    }

    @Override
    public synchronized List<String> getAllAuthsByName(PlayerAuth auth) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        List<String> countIp = new ArrayList<>();
        try {
            con = getConnection();
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnIp + "=?;");
            pst.setString(1, auth.getIp());
            rs = pst.executeQuery();
            while (rs.next()) {
                countIp.add(rs.getString(columnName));
            }
            return countIp;
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<>();
        } finally {
            close(rs);
            close(pst);
            close(con);
        }
    }

    @Override
    public synchronized List<String> getAllAuthsByIp(String ip) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        List<String> countIp = new ArrayList<>();
        try {
            con = getConnection();
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnIp + "=?;");
            pst.setString(1, ip);
            rs = pst.executeQuery();
            while (rs.next()) {
                countIp.add(rs.getString(columnName));
            }
            return countIp;
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<>();
        } finally {
            close(rs);
            close(pst);
            close(con);
        }
    }

    @Override
    public synchronized List<String> getAllAuthsByEmail(String email) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        List<String> countEmail = new ArrayList<>();
        try {
            con = getConnection();
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnEmail + "=?;");
            pst.setString(1, email);
            rs = pst.executeQuery();
            while (rs.next()) {
                countEmail.add(rs.getString(columnName));
            }
            return countEmail;
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<>();
        } finally {
            close(rs);
            close(pst);
            close(con);
        }
    }

    @Override
    public synchronized void purgeBanned(List<String> banned) {
        Connection con = null;
        PreparedStatement pst = null;
        try {
            for (String name : banned) {
                con = getConnection();
                pst = con.prepareStatement("DELETE FROM " + tableName + " WHERE LOWER(" + columnName + ")=?;");
                pst.setString(1, name);
                pst.executeUpdate();
            }
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
        } finally {
            close(pst);
            close(con);
        }
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.MYSQL;
    }

    @Override
    public boolean isLogged(String user) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            con = getConnection();
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE LOWER(" + columnName + ")=?;");
            pst.setString(1, user);
            rs = pst.executeQuery();
            if (rs.next())
                return (rs.getInt(columnLogged) == 1);
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(rs);
            close(pst);
            close(con);
        }
        return false;
    }

    @Override
    public void setLogged(String user) {
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = getConnection();
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnLogged + "=? WHERE LOWER(" + columnName + ")=?;");
            pst.setInt(1, 1);
            pst.setString(2, user);
            pst.executeUpdate();
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
        } finally {
            close(pst);
            close(con);
        }
    }

    @Override
    public void setUnlogged(String user) {
        Connection con = null;
        PreparedStatement pst = null;
        if (user != null)
            try {
                con = getConnection();
                pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnLogged + "=? WHERE LOWER(" + columnName + ")=?;");
                pst.setInt(1, 0);
                pst.setString(2, user);
                pst.executeUpdate();
            } catch (Exception ex) {
                ConsoleLogger.showError(ex.getMessage());
            } finally {
                close(pst);
                close(con);
            }
    }

    @Override
    public void purgeLogged() {
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = getConnection();
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnLogged + "=? WHERE " + columnLogged + "=?;");
            pst.setInt(1, 0);
            pst.setInt(2, 1);
            pst.executeUpdate();
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
        } finally {
            close(pst);
            close(con);
        }
    }

    @Override
    public int getAccountsRegistered() {
        int result = 0;
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs;
        try {
            con = getConnection();
            pst = con.prepareStatement("SELECT COUNT(*) FROM " + tableName + ";");
            rs = pst.executeQuery();
            if (rs != null && rs.next()) {
                result = rs.getInt(1);
            }
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            return result;
        } finally {
            close(pst);
            close(con);
        }
        return result;
    }

    @Override
    public void updateName(String oldone, String newone) {
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = getConnection();
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnName + "=? WHERE LOWER(" + columnName + ")=?;");
            pst.setString(1, newone);
            pst.setString(2, oldone);
            pst.executeUpdate();
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
        } finally {
            close(pst);
            close(con);
        }
    }

    @Override
    public List<PlayerAuth> getAllAuths() {
        List<PlayerAuth> auths = new ArrayList<>();
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            con = getConnection();
            pst = con.prepareStatement("SELECT * FROM " + tableName + ";");
            rs = pst.executeQuery();
            while (rs.next()) {
                PlayerAuth pAuth;
                int id = rs.getInt(columnID);
                if (rs.getString(columnIp).isEmpty() && rs.getString(columnIp) != null) {
                    pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), "192.168.0.1", rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail), rs.getString(columnRealName));
                } else {
                    if (!columnSalt.isEmpty()) {
                        if (!columnGroup.isEmpty())
                            pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), rs.getString(columnSalt), rs.getInt(columnGroup), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail), rs.getString(columnRealName));
                        else
                            pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), rs.getString(columnSalt), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail), rs.getString(columnRealName));
                    } else {
                        pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail), rs.getString(columnRealName));
                    }
                }
                if (Settings.getPasswordHash == HashAlgorithm.XENFORO) {
                    ResultSet rsid;
                    pst = con.prepareStatement("SELECT * FROM xf_user_authenticate WHERE " + columnID + "=?;");
                    pst.setInt(1, id);
                    rsid = pst.executeQuery();
                    if (rsid.next()) {
                        Blob blob = rsid.getBlob("data");
                        byte[] bytes = blob.getBytes(1, (int) blob.length());
                        pAuth.setHash(new String(bytes));
                    }
                    rsid.close();
                }
                auths.add(pAuth);
            }
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            return auths;
        } finally {
            close(pst);
            close(con);
            close(rs);
        }
        return auths;
    }

    @Override
    public List<PlayerAuth> getLoggedPlayers() {
        List<PlayerAuth> auths = new ArrayList<>();
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            con = getConnection();
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnLogged + "=1;");
            rs = pst.executeQuery();
            while (rs.next()) {
                PlayerAuth pAuth;
                int id = rs.getInt(columnID);
                if (rs.getString(columnIp).isEmpty() && rs.getString(columnIp) != null) {
                    pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), "192.168.0.1", rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail), rs.getString(columnRealName));
                } else {
                    if (!columnSalt.isEmpty()) {
                        if (!columnGroup.isEmpty())
                            pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), rs.getString(columnSalt), rs.getInt(columnGroup), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail), rs.getString(columnRealName));
                        else
                            pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), rs.getString(columnSalt), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail), rs.getString(columnRealName));
                    } else {
                        pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail), rs.getString(columnRealName));
                    }
                }
                if (Settings.getPasswordHash == HashAlgorithm.XENFORO) {
                    ResultSet rsid;
                    pst = con.prepareStatement("SELECT * FROM xf_user_authenticate WHERE " + columnID + "=?;");
                    pst.setInt(1, id);
                    rsid = pst.executeQuery();
                    if (rsid.next()) {
                        Blob blob = rsid.getBlob("data");
                        byte[] bytes = blob.getBytes(1, (int) blob.length());
                        pAuth.setHash(new String(bytes));
                    }
                    rsid.close();
                }
                auths.add(pAuth);
            }
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            return auths;
        } finally {
            close(pst);
            close(rs);
            close(con);
        }
        return auths;
    }

}
