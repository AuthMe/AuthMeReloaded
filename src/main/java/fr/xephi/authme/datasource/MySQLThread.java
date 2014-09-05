package fr.xephi.authme.datasource;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.MiniConnectionPoolManager.TimeoutException;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.settings.Settings;

public class MySQLThread extends Thread implements DataSource {

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
    private MiniConnectionPoolManager conPool;

    public void run() {
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
        try {
            this.connect();
            this.setup();
        } catch (ClassNotFoundException e) {
            ConsoleLogger.showError(e.getMessage());
            if (Settings.isStopEnabled) {
                ConsoleLogger.showError("Can't use MySQL... Please input correct MySQL informations ! SHUTDOWN...");
                AuthMe.getInstance().getServer().shutdown();
            }
            if (!Settings.isStopEnabled)
                AuthMe.getInstance().getServer().getPluginManager().disablePlugin(AuthMe.getInstance());
            return;
        } catch (SQLException e) {
            ConsoleLogger.showError(e.getMessage());
            if (Settings.isStopEnabled) {
                ConsoleLogger.showError("Can't use MySQL... Please input correct MySQL informations ! SHUTDOWN...");
                AuthMe.getInstance().getServer().shutdown();
            }
            if (!Settings.isStopEnabled)
                AuthMe.getInstance().getServer().getPluginManager().disablePlugin(AuthMe.getInstance());
            return;
        } catch (TimeoutException e) {
            ConsoleLogger.showError(e.getMessage());
            if (Settings.isStopEnabled) {
                ConsoleLogger.showError("Can't use MySQL... Please input correct MySQL informations ! SHUTDOWN...");
                AuthMe.getInstance().getServer().shutdown();
            }
            if (!Settings.isStopEnabled)
                AuthMe.getInstance().getServer().getPluginManager().disablePlugin(AuthMe.getInstance());
            return;
        }
    }

    private synchronized void connect() throws ClassNotFoundException,
            SQLException, TimeoutException {
        Class.forName("com.mysql.jdbc.Driver");
        ConsoleLogger.info("MySQL driver loaded");
        MysqlConnectionPoolDataSource dataSource = new MysqlConnectionPoolDataSource();
        dataSource.setDatabaseName(database);
        dataSource.setServerName(host);
        dataSource.setPort(Integer.parseInt(port));
        dataSource.setUser(username);
        dataSource.setPassword(password);
        conPool = new MiniConnectionPoolManager(dataSource, 10);
        ConsoleLogger.info("Connection pool ready");
    }

    private synchronized void setup() throws SQLException {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            con = makeSureConnectionIsReady();
            st = con.createStatement();
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + tableName + " (" + columnID + " INTEGER AUTO_INCREMENT," + columnName + " VARCHAR(255) NOT NULL UNIQUE," + columnPassword + " VARCHAR(255) NOT NULL," + columnIp + " VARCHAR(40) NOT NULL DEFAULT '127.0.0.1'," + columnLastLogin + " BIGINT NOT NULL DEFAULT '" + System.currentTimeMillis() + "'," + lastlocX + " DOUBLE NOT NULL DEFAULT '0.0'," + lastlocY + " DOUBLE NOT NULL DEFAULT '0.0'," + lastlocZ + " DOUBLE NOT NULL DEFAULT '0.0'," + lastlocWorld + " VARCHAR(255) DEFAULT 'world'," + columnEmail + " VARCHAR(255) DEFAULT 'your@email.com'," + columnLogged + " SMALLINT NOT NULL DEFAULT '0'," + "CONSTRAINT table_const_prim PRIMARY KEY (" + columnID + "));");
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
        } finally {
            close(rs);
            close(st);
            close(con);
        }
    }

    @Override
    public synchronized boolean isAuthAvailable(String user) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            con = makeSureConnectionIsReady();
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE LOWER(" + columnName + ")=LOWER(?);");
            pst.setString(1, user);
            rs = pst.executeQuery();
            return rs.next();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } catch (TimeoutException ex) {
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
        int id = -1;
        try {
            con = makeSureConnectionIsReady();
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE LOWER(" + columnName + ")=LOWER(?);");
            pst.setString(1, user);
            rs = pst.executeQuery();
            if (rs.next()) {
                id = rs.getInt(columnID);
                if (rs.getString(columnIp).isEmpty() && rs.getString(columnIp) != null) {
                    pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), "198.18.0.1", rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail));
                } else {
                    if (!columnSalt.isEmpty()) {
                        if (!columnGroup.isEmpty())
                            pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), rs.getString(columnSalt), rs.getInt(columnGroup), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail));
                        else pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), rs.getString(columnSalt), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail));
                    } else {
                        pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail));
                    }
                }
                if (Settings.getPasswordHash == HashAlgorithm.XENFORO) {
                    rs.close();
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
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return null;
        } catch (TimeoutException ex) {
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
        try {
            con = makeSureConnectionIsReady();
            if ((columnSalt == null || columnSalt.isEmpty()) || (auth.getSalt() == null || auth.getSalt().isEmpty())) {
                pst = con.prepareStatement("INSERT INTO " + tableName + "(" + columnName + "," + columnPassword + "," + columnIp + "," + columnLastLogin + ") VALUES (?,?,?,?);");
                pst.setString(1, auth.getNickname());
                pst.setString(2, auth.getHash());
                pst.setString(3, auth.getIp());
                pst.setLong(4, auth.getLastLogin());
                pst.executeUpdate();
            } else {
                pst = con.prepareStatement("INSERT INTO " + tableName + "(" + columnName + "," + columnPassword + "," + columnIp + "," + columnLastLogin + "," + columnSalt + ") VALUES (?,?,?,?,?);");
                pst.setString(1, auth.getNickname());
                pst.setString(2, auth.getHash());
                pst.setString(3, auth.getIp());
                pst.setLong(4, auth.getLastLogin());
                pst.setString(5, auth.getSalt());
                pst.executeUpdate();
            }
            if (!columnOthers.isEmpty()) {
                for (String column : columnOthers) {
                    pst = con.prepareStatement("UPDATE " + tableName + " SET " + column + "=? WHERE " + columnName + "=?;");
                    pst.setString(1, auth.getNickname());
                    pst.setString(2, auth.getNickname());
                    pst.executeUpdate();
                }
            }
            if (Settings.getPasswordHash == HashAlgorithm.PHPBB) {
                int id;
                ResultSet rs = null;
                pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnName + "=?;");
                pst.setString(1, auth.getNickname());
                rs = pst.executeQuery();
                if (rs.next()) {
                    id = rs.getInt(columnID);
                    // Insert player in phpbb_user_group
                    pst = con.prepareStatement("INSERT INTO " + Settings.getPhpbbPrefix + "user_group (group_id, user_id, group_leader, user_pending) VALUES (?,?,?,?);");
                    pst.setInt(1, Settings.getPhpbbGroup);
                    pst.setInt(2, id);
                    pst.setInt(3, 0);
                    pst.setInt(4, 0);
                    pst.executeUpdate();
                    // Update player group in phpbb_users
                    pst = con.prepareStatement("UPDATE " + tableName + " SET " + tableName + ".group_id=? WHERE " + columnName + "=?;");
                    pst.setInt(1, Settings.getPhpbbGroup);
                    pst.setString(2, auth.getNickname());
                    pst.executeUpdate();
                    // Get current time without ms
                    long time = System.currentTimeMillis() / 1000;
                    // Update user_regdate
                    pst = con.prepareStatement("UPDATE " + tableName + " SET " + tableName + ".user_regdate=? WHERE " + columnName + "=?;");
                    pst.setLong(1, time);
                    pst.setString(2, auth.getNickname());
                    pst.executeUpdate();
                    // Update user_lastvisit
                    pst = con.prepareStatement("UPDATE " + tableName + " SET " + tableName + ".user_lastvisit=? WHERE " + columnName + "=?;");
                    pst.setLong(1, time);
                    pst.setString(2, auth.getNickname());
                    pst.executeUpdate();
                }
            }
            if (Settings.getPasswordHash == HashAlgorithm.WORDPRESS) {
                int id;
                ResultSet rs = null;
                pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnName + "=?;");
                pst.setString(1, auth.getNickname());
                rs = pst.executeQuery();
                if (rs.next()) {
                    id = rs.getInt(columnID);
                    // First Name
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "first_name");
                    pst.setString(3, "");
                    pst.executeUpdate();
                    // Last Name
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "last_name");
                    pst.setString(3, "");
                    pst.executeUpdate();
                    // Nick Name
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "nickname");
                    pst.setString(3, auth.getNickname());
                    pst.executeUpdate();
                    // Description
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "description");
                    pst.setString(3, "");
                    pst.executeUpdate();
                    // Rich_Editing
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "rich_editing");
                    pst.setString(3, "true");
                    pst.executeUpdate();
                    // Comments_Shortcuts
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "comment_shortcuts");
                    pst.setString(3, "false");
                    pst.executeUpdate();
                    // admin_color
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "admin_color");
                    pst.setString(3, "fresh");
                    pst.executeUpdate();
                    // use_ssl
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "use_ssl");
                    pst.setString(3, "0");
                    pst.executeUpdate();
                    // show_admin_bar_front
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "show_admin_bar_front");
                    pst.setString(3, "true");
                    pst.executeUpdate();
                    // wp_capabilities
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "wp_capabilities");
                    pst.setString(3, "a:1:{s:10:\"subscriber\";b:1;}");
                    pst.executeUpdate();
                    // wp_user_level
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "wp_user_level");
                    pst.setString(3, "0");
                    pst.executeUpdate();
                    // default_password_nag
                    pst = con.prepareStatement("INSERT INTO " + Settings.getWordPressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?);");
                    pst.setInt(1, id);
                    pst.setString(2, "default_password_nag");
                    pst.setString(3, "");
                    pst.executeUpdate();
                }
            }
            if (Settings.getPasswordHash == HashAlgorithm.XENFORO) {
                int id;
                ResultSet rs = null;
                pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnName + "=?;");
                pst.setString(1, auth.getNickname());
                rs = pst.executeQuery();
                if (rs.next()) {
                    id = rs.getInt(columnID);
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
            }
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } catch (TimeoutException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(pst);
            close(con);
        }
        return true;
    }

    @Override
    public synchronized boolean updatePassword(PlayerAuth auth) {
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = makeSureConnectionIsReady();
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnPassword + "=? WHERE " + columnName + "=?;");
            pst.setString(1, auth.getHash());
            pst.setString(2, auth.getNickname());
            pst.executeUpdate();
            if (Settings.getPasswordHash == HashAlgorithm.XENFORO) {
                int id;
                ResultSet rs = null;
                pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnName + "=?;");
                pst.setString(1, auth.getNickname());
                rs = pst.executeQuery();
                if (rs.next()) {
                    id = rs.getInt(columnID);
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
            }
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } catch (TimeoutException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
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
            con = makeSureConnectionIsReady();
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnIp + "=?, " + columnLastLogin + "=? WHERE " + columnName + "=?;");
            pst.setString(1, auth.getIp());
            pst.setLong(2, auth.getLastLogin());
            pst.setString(3, auth.getNickname());
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } catch (TimeoutException ex) {
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
            con = makeSureConnectionIsReady();
            pst = con.prepareStatement("DELETE FROM " + tableName + " WHERE " + columnLastLogin + "<?;");
            pst.setLong(1, until);
            return pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return 0;
        } catch (TimeoutException ex) {
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
        List<String> list = new ArrayList<String>();
        try {
            con = makeSureConnectionIsReady();
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnLastLogin + "<?;");
            pst.setLong(1, until);
            rs = pst.executeQuery();
            while (rs.next()) {
                list.add(rs.getString(columnName));
            }
            pst = con.prepareStatement("DELETE FROM " + tableName + " WHERE " + columnLastLogin + "<?;");
            pst.setLong(1, until);
            pst.executeUpdate();
            return list;
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<String>();
        } catch (TimeoutException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<String>();
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
            con = makeSureConnectionIsReady();
            if (Settings.getPasswordHash == HashAlgorithm.XENFORO) {
                int id;
                ResultSet rs = null;
                pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnName + "=?;");
                pst.setString(1, user);
                rs = pst.executeQuery();
                if (rs.next()) {
                    id = rs.getInt(columnID);
                    // Remove data
                    pst = con.prepareStatement("DELETE FROM xf_user_authenticate WHERE " + columnID + "=?;");
                    pst.setInt(1, id);
                }
            }
            pst = con.prepareStatement("DELETE FROM " + tableName + " WHERE " + columnName + "=?;");
            pst.setString(1, user);
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } catch (TimeoutException ex) {
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
            con = makeSureConnectionIsReady();
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + lastlocX + " =?, " + lastlocY + "=?, " + lastlocZ + "=?, " + lastlocWorld + "=? WHERE " + columnName + "=?;");
            pst.setDouble(1, auth.getQuitLocX());
            pst.setDouble(2, auth.getQuitLocY());
            pst.setDouble(3, auth.getQuitLocZ());
            pst.setString(4, auth.getWorld());
            pst.setString(5, auth.getNickname());
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } catch (TimeoutException ex) {
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
            con = makeSureConnectionIsReady();
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnIp + "=?;");
            pst.setString(1, ip);
            rs = pst.executeQuery();
            while (rs.next()) {
                countIp++;
            }
            return countIp;
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return 0;
        } catch (TimeoutException ex) {
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
            con = makeSureConnectionIsReady();
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnEmail + " =? WHERE " + columnName + "=?;");
            pst.setString(1, auth.getEmail());
            pst.setString(2, auth.getNickname());
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } catch (TimeoutException ex) {
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
            con = makeSureConnectionIsReady();
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnSalt + " =? WHERE " + columnName + "=?;");
            pst.setString(1, auth.getSalt());
            pst.setString(2, auth.getNickname());
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } catch (TimeoutException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(pst);
            close(con);
        }
        return true;
    }

    @Override
    public synchronized void close() {
        try {
            conPool.dispose();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
        }
    }

    @Override
    public void reload() {
        try {
            reconnect(true);
        } catch (Exception e) {
            ConsoleLogger.showError(e.getMessage());
            if (Settings.isStopEnabled) {
                ConsoleLogger.showError("Can't reconnect to MySQL database... Please check your MySQL informations ! SHUTDOWN...");
                AuthMe.getInstance().getServer().shutdown();
            }
            if (!Settings.isStopEnabled)
                AuthMe.getInstance().getServer().getPluginManager().disablePlugin(AuthMe.getInstance());
        }
    }

    private void close(Statement st) {
        if (st != null) {
            try {
                st.close();
            } catch (SQLException ex) {
                ConsoleLogger.showError(ex.getMessage());
            }
        }
    }

    private void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ex) {
                ConsoleLogger.showError(ex.getMessage());
            }
        }
    }

    private void close(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException ex) {
                ConsoleLogger.showError(ex.getMessage());
            }
        }
    }

    @Override
    public synchronized List<String> getAllAuthsByName(PlayerAuth auth) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        List<String> countIp = new ArrayList<String>();
        try {
            con = makeSureConnectionIsReady();
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnIp + "=?;");
            pst.setString(1, auth.getIp());
            rs = pst.executeQuery();
            while (rs.next()) {
                countIp.add(rs.getString(columnName));
            }
            return countIp;
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<String>();
        } catch (TimeoutException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<String>();
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
        List<String> countIp = new ArrayList<String>();
        try {
            con = makeSureConnectionIsReady();
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnIp + "=?;");
            pst.setString(1, ip);
            rs = pst.executeQuery();
            while (rs.next()) {
                countIp.add(rs.getString(columnName));
            }
            return countIp;
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<String>();
        } catch (TimeoutException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<String>();
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
        List<String> countEmail = new ArrayList<String>();
        try {
            con = makeSureConnectionIsReady();
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnEmail + "=?;");
            pst.setString(1, email);
            rs = pst.executeQuery();
            while (rs.next()) {
                countEmail.add(rs.getString(columnName));
            }
            return countEmail;
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<String>();
        } catch (TimeoutException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<String>();
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
                con = makeSureConnectionIsReady();
                pst = con.prepareStatement("DELETE FROM " + tableName + " WHERE " + columnName + "=?;");
                pst.setString(1, name);
                pst.executeUpdate();
            }
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
        } finally {
            close(pst);
            close(con);
        }
    }

    private synchronized Connection makeSureConnectionIsReady() {
        Connection con = null;
        try {
            con = conPool.getValidConnection();
        } catch (Exception te) {
            try {
                con = null;
                reconnect(false);
            } catch (Exception e) {
                ConsoleLogger.showError(e.getMessage());
                if (Settings.isStopEnabled) {
                    ConsoleLogger.showError("Can't reconnect to MySQL database... Please check your MySQL informations ! SHUTDOWN...");
                    AuthMe.getInstance().getServer().shutdown();
                }
                if (!Settings.isStopEnabled)
                    AuthMe.getInstance().getServer().getPluginManager().disablePlugin(AuthMe.getInstance());
            }
        } catch (AssertionError ae) {
            // Make sure assertionerror is caused by the connectionpoolmanager,
            // else re-throw it
            if (!ae.getMessage().equalsIgnoreCase("AuthMeDatabaseError"))
                throw new AssertionError(ae.getMessage());
            try {
                con = null;
                reconnect(false);
            } catch (Exception e) {
                ConsoleLogger.showError(e.getMessage());
                if (Settings.isStopEnabled) {
                    ConsoleLogger.showError("Can't reconnect to MySQL database... Please check your MySQL informations ! SHUTDOWN...");
                    AuthMe.getInstance().getServer().shutdown();
                }
                if (!Settings.isStopEnabled)
                    AuthMe.getInstance().getServer().getPluginManager().disablePlugin(AuthMe.getInstance());
            }
        }
        if (con == null)
            con = conPool.getValidConnection();
        return con;
    }

    private synchronized void reconnect(boolean reload)
            throws ClassNotFoundException, SQLException, TimeoutException {
        conPool.dispose();
        Class.forName("com.mysql.jdbc.Driver");
        MysqlConnectionPoolDataSource dataSource = new MysqlConnectionPoolDataSource();
        dataSource.setDatabaseName(database);
        dataSource.setServerName(host);
        dataSource.setPort(Integer.parseInt(port));
        dataSource.setUser(username);
        dataSource.setPassword(password);
        conPool = new MiniConnectionPoolManager(dataSource, 10);
        if (!reload)
            ConsoleLogger.info("ConnectionPool was unavailable... Reconnected!");
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
            con = makeSureConnectionIsReady();
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnName + "=?;");
            pst.setString(1, user);
            rs = pst.executeQuery();
            if (rs.next())
                return (rs.getInt(columnLogged) == 1);
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } catch (TimeoutException ex) {
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
            con = makeSureConnectionIsReady();
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnLogged + "=? WHERE " + columnName + "=?;");
            pst.setInt(1, 1);
            pst.setString(2, user);
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return;
        } catch (TimeoutException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return;
        } finally {
            close(pst);
            close(con);
        }
        return;
    }

    @Override
    public void setUnlogged(String user) {
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = makeSureConnectionIsReady();
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnLogged + "=? WHERE " + columnName + "=?;");
            pst.setInt(1, 0);
            pst.setString(2, user);
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return;
        } catch (TimeoutException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return;
        } finally {
            close(pst);
            close(con);
        }
        return;
    }

    @Override
    public void purgeLogged() {
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = makeSureConnectionIsReady();
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnLogged + "=? WHERE " + columnLogged + "=?;");
            pst.setInt(1, 0);
            pst.setInt(2, 1);
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return;
        } catch (TimeoutException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return;
        } finally {
            close(pst);
            close(con);
        }
        return;
    }

    @Override
    public int getAccountsRegistered() {
        int result = 0;
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            con = makeSureConnectionIsReady();
            pst = con.prepareStatement("SELECT COUNT(*) FROM " + tableName + ";");
            rs = pst.executeQuery();
            if (rs != null && rs.next()) {
                result = rs.getInt(1);
            }
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return result;
        } catch (TimeoutException ex) {
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
            con = makeSureConnectionIsReady();
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnName + "=? WHERE " + columnName + "=?;");
            pst.setString(1, newone);
            pst.setString(2, oldone);
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return;
        } catch (TimeoutException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return;
        } finally {
            close(pst);
            close(con);
        }
        return;
    }

    @Override
    public List<PlayerAuth> getAllAuths() {
        List<PlayerAuth> auths = new ArrayList<PlayerAuth>();
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            con = makeSureConnectionIsReady();
            pst = con.prepareStatement("SELECT * FROM " + tableName + ";");
            rs = pst.executeQuery();
            while (rs.next()) {
                PlayerAuth pAuth = null;
                int id = rs.getInt(columnID);
                if (rs.getString(columnIp).isEmpty() && rs.getString(columnIp) != null) {
                    pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), "198.18.0.1", rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail));
                } else {
                    if (!columnSalt.isEmpty()) {
                        if (!columnGroup.isEmpty())
                            pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), rs.getString(columnSalt), rs.getInt(columnGroup), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail));
                        else pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), rs.getString(columnSalt), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail));
                    } else {
                        pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail));
                    }
                }
                if (Settings.getPasswordHash == HashAlgorithm.XENFORO) {
                    rs.close();
                    pst = con.prepareStatement("SELECT * FROM xf_user_authenticate WHERE " + columnID + "=?;");
                    pst.setInt(1, id);
                    rs = pst.executeQuery();
                    if (rs.next()) {
                        Blob blob = rs.getBlob("data");
                        byte[] bytes = blob.getBytes(1, (int) blob.length());
                        pAuth.setHash(new String(bytes));
                    }
                }
                if (pAuth != null)
                    auths.add(pAuth);
            }
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return auths;
        } catch (TimeoutException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return auths;
        } finally {
            close(pst);
            close(con);
        }
        return auths;
    }

}
