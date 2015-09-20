package fr.xephi.authme.datasource;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.settings.Settings;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SQLite implements DataSource {

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
    private Connection con;
    private String columnLogged;
    private String columnRealName;

    public SQLite() throws ClassNotFoundException, SQLException {
        this.database = Settings.getMySQLDatabase;
        this.tableName = Settings.getMySQLTablename;
        this.columnName = Settings.getMySQLColumnName;
        this.columnPassword = Settings.getMySQLColumnPassword;
        this.columnIp = Settings.getMySQLColumnIp;
        this.columnLastLogin = Settings.getMySQLColumnLastLogin;
        this.columnSalt = Settings.getMySQLColumnSalt;
        this.columnGroup = Settings.getMySQLColumnGroup;
        this.lastlocX = Settings.getMySQLlastlocX;
        this.lastlocY = Settings.getMySQLlastlocY;
        this.lastlocZ = Settings.getMySQLlastlocZ;
        this.lastlocWorld = Settings.getMySQLlastlocWorld;
        this.columnEmail = Settings.getMySQLColumnEmail;
        this.columnID = Settings.getMySQLColumnId;
        this.columnLogged = Settings.getMySQLColumnLogged;
        this.columnRealName = Settings.getMySQLColumnRealName;

        try {
            this.connect();
            this.setup();
        } catch (ClassNotFoundException | SQLException cnf) {
            ConsoleLogger.showError("Can't use SQLITE... !");
            throw cnf;
        }
    }

    private synchronized void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        ConsoleLogger.info("SQLite driver loaded");
        this.con = DriverManager.getConnection("jdbc:sqlite:plugins/AuthMe/" + database + ".db");

    }

    private synchronized void setup() throws SQLException {
        Statement st = null;
        ResultSet rs = null;
        try {
            st = con.createStatement();
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + tableName + " (" + columnID + " INTEGER AUTO_INCREMENT," + columnName + " VARCHAR(255) NOT NULL UNIQUE," + columnPassword + " VARCHAR(255) NOT NULL," + columnIp + " VARCHAR(40) NOT NULL," + columnLastLogin + " BIGINT," + lastlocX + " DOUBLE NOT NULL DEFAULT '0.0'," + lastlocY + " DOUBLE NOT NULL DEFAULT '0.0'," + lastlocZ + " DOUBLE NOT NULL DEFAULT '0.0'," + lastlocWorld + " VARCHAR(255) NOT NULL DEFAULT '" + Settings.defaultWorld + "'," + columnEmail + " VARCHAR(255) DEFAULT 'your@email.com'," + "CONSTRAINT table_const_prim PRIMARY KEY (" + columnID + "));");
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
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnLastLogin + " BIGINT DEFAULT '0';");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, lastlocX);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + lastlocX + " DOUBLE NOT NULL DEFAULT '0.0';");
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + lastlocY + " DOUBLE NOT NULL DEFAULT '0.0';");
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + lastlocZ + " DOUBLE NOT NULL DEFAULT '0.0';");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, lastlocWorld);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + lastlocWorld + " VARCHAR(255) NOT NULL DEFAULT 'world';");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, columnEmail);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnEmail + " VARCHAR(255) DEFAULT 'your@email.com';");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, columnLogged);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnLogged + " BIGINT DEFAULT '0';");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, columnRealName);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnRealName + " VARCHAR(255) NOT NULL DEFAULT 'Player';");
            }
        } finally {
            close(rs);
            close(st);
        }
        ConsoleLogger.info("SQLite Setup finished");
    }

    @Override
    public synchronized boolean isAuthAvailable(String user) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE LOWER(" + columnName + ")=LOWER(?);");
            pst.setString(1, user);
            rs = pst.executeQuery();
            return rs.next();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(rs);
            close(pst);
        }
    }

    @Override
    public synchronized PlayerAuth getAuth(String user) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE LOWER(" + columnName + ")=LOWER(?);");
            pst.setString(1, user);
            rs = pst.executeQuery();
            if (rs.next()) {
                if (rs.getString(columnIp).isEmpty()) {
                    return new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), "192.168.0.1", rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail), rs.getString(columnRealName));
                } else {
                    if (!columnSalt.isEmpty()) {
                        return new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), rs.getString(columnSalt), rs.getInt(columnGroup), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail), rs.getString(columnRealName));
                    } else {
                        return new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail), rs.getString(columnRealName));
                    }
                }
            } else {
                return null;
            }
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return null;
        } finally {
            close(rs);
            close(pst);
        }
    }

    @Override
    public synchronized boolean saveAuth(PlayerAuth auth) {
        PreparedStatement pst = null;
        try {
            if (columnSalt.isEmpty() && auth.getSalt().isEmpty()) {
                pst = con.prepareStatement("INSERT INTO " + tableName + "(" + columnName + "," + columnPassword + "," + columnIp + "," + columnLastLogin + "," + columnRealName + ") VALUES (?,?,?,?,?);");
                pst.setString(1, auth.getNickname());
                pst.setString(2, auth.getHash());
                pst.setString(3, auth.getIp());
                pst.setLong(4, auth.getLastLogin());
                pst.setString(5, auth.getRealName());
                pst.executeUpdate();
            } else {
                pst = con.prepareStatement("INSERT INTO " + tableName + "(" + columnName + "," + columnPassword + "," + columnIp + "," + columnLastLogin + "," + columnSalt + "," + columnRealName + ") VALUES (?,?,?,?,?,?);");
                pst.setString(1, auth.getNickname());
                pst.setString(2, auth.getHash());
                pst.setString(3, auth.getIp());
                pst.setLong(4, auth.getLastLogin());
                pst.setString(5, auth.getSalt());
                pst.setString(6, auth.getRealName());
                pst.executeUpdate();
            }
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(pst);
        }
        return true;
    }

    @Override
    public synchronized boolean updatePassword(PlayerAuth auth) {
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnPassword + "=? WHERE " + columnName + "=?;");
            pst.setString(1, auth.getHash());
            pst.setString(2, auth.getNickname());
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(pst);
        }
        return true;
    }

    @Override
    public boolean updateSession(PlayerAuth auth) {
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnIp + "=?, " + columnLastLogin + "=?, " + columnRealName + "=? WHERE " + columnName + "=?;");
            pst.setString(1, auth.getIp());
            pst.setLong(2, auth.getLastLogin());
            pst.setString(3, auth.getRealName());
            pst.setString(4, auth.getNickname());
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(pst);
        }
        return true;
    }

    @Override
    public int purgeDatabase(long until) {
        PreparedStatement pst = null;
        try {

            pst = con.prepareStatement("DELETE FROM " + tableName + " WHERE " + columnLastLogin + "<?;");
            pst.setLong(1, until);
            return pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return 0;
        } finally {
            close(pst);
        }
    }

    @Override
    public List<String> autoPurgeDatabase(long until) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        List<String> list = new ArrayList<>();
        try {
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnLastLogin + "<?;");
            pst.setLong(1, until);
            rs = pst.executeQuery();
            while (rs.next()) {
                list.add(rs.getString(columnName));
            }
            return list;
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<>();
        } finally {
            close(rs);
            close(pst);
        }
    }

    @Override
    public synchronized boolean removeAuth(String user) {
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("DELETE FROM " + tableName + " WHERE " + columnName + "=?;");
            pst.setString(1, user);
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(pst);
        }
        return true;
    }

    @Override
    public boolean updateQuitLoc(PlayerAuth auth) {
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + lastlocX + "=?, " + lastlocY + "=?, " + lastlocZ + "=?, " + lastlocWorld + "=? WHERE " + columnName + "=?;");
            pst.setDouble(1, auth.getQuitLocX());
            pst.setDouble(2, auth.getQuitLocY());
            pst.setDouble(3, auth.getQuitLocZ());
            pst.setString(4, auth.getWorld());
            pst.setString(5, auth.getNickname());
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(pst);
        }
        return true;
    }

    @Override
    public int getIps(String ip) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        int countIp = 0;
        try {
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
        } finally {
            close(rs);
            close(pst);
        }
    }

    @Override
    public boolean updateEmail(PlayerAuth auth) {
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnEmail + "=? WHERE " + columnName + "=?;");
            pst.setString(1, auth.getEmail());
            pst.setString(2, auth.getNickname());
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(pst);
        }
        return true;
    }

    @Override
    public boolean updateSalt(PlayerAuth auth) {
        if (columnSalt.isEmpty()) {
            return false;
        }
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnSalt + "=? WHERE " + columnName + "=?;");
            pst.setString(1, auth.getSalt());
            pst.setString(2, auth.getNickname());
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(pst);
        }
        return true;
    }

    @Override
    public synchronized void close() {
        try {
            con.close();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
        }
    }

    @Override
    public void reload() {
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

    @Override
    public List<String> getAllAuthsByName(PlayerAuth auth) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        List<String> countIp = new ArrayList<>();
        try {
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnIp + "=?;");
            pst.setString(1, auth.getIp());
            rs = pst.executeQuery();
            while (rs.next()) {
                countIp.add(rs.getString(columnName));
            }
            return countIp;
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<>();
        } catch (NullPointerException npe) {
            return new ArrayList<>();
        } finally {
            close(rs);
            close(pst);
        }
    }

    @Override
    public List<String> getAllAuthsByIp(String ip) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        List<String> countIp = new ArrayList<>();
        try {
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnIp + "=?;");
            pst.setString(1, ip);
            rs = pst.executeQuery();
            while (rs.next()) {
                countIp.add(rs.getString(columnName));
            }
            return countIp;
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<>();
        } catch (NullPointerException npe) {
            return new ArrayList<>();
        } finally {
            close(rs);
            close(pst);
        }
    }

    @Override
    public List<String> getAllAuthsByEmail(String email) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        List<String> countEmail = new ArrayList<>();
        try {
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnEmail + "=?;");
            pst.setString(1, email);
            rs = pst.executeQuery();
            while (rs.next()) {
                countEmail.add(rs.getString(columnName));
            }
            return countEmail;
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<>();
        } catch (NullPointerException npe) {
            return new ArrayList<>();
        } finally {
            close(rs);
            close(pst);
        }
    }

    @Override
    public void purgeBanned(List<String> banned) {
        PreparedStatement pst = null;
        try {
            for (String name : banned) {
                pst = con.prepareStatement("DELETE FROM " + tableName + " WHERE " + columnName + "=?;");
                pst.setString(1, name);
                pst.executeUpdate();
            }
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
        } finally {
            close(pst);
        }
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.SQLITE;
    }

    @Override
    public boolean isLogged(String user) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE LOWER(" + columnName + ")=?;");
            pst.setString(1, user);
            rs = pst.executeQuery();
            if (rs.next())
                return (rs.getInt(columnLogged) == 1);
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(rs);
            close(pst);
        }
        return false;
    }

    @Override
    public void setLogged(String user) {
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnLogged + "=? WHERE LOWER(" + columnName + ")=?;");
            pst.setInt(1, 1);
            pst.setString(2, user);
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
        } finally {
            close(pst);
        }
    }

    @Override
    public void setUnlogged(String user) {
        PreparedStatement pst = null;
        if (user != null)
            try {
                pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnLogged + "=? WHERE LOWER(" + columnName + ")=?;");
                pst.setInt(1, 0);
                pst.setString(2, user);
                pst.executeUpdate();
            } catch (SQLException ex) {
                ConsoleLogger.showError(ex.getMessage());
            } finally {
                close(pst);
            }
    }

    @Override
    public void purgeLogged() {
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnLogged + "=? WHERE " + columnLogged + "=?;");
            pst.setInt(1, 0);
            pst.setInt(2, 1);
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
        } finally {
            close(pst);
        }
    }

    @Override
    public int getAccountsRegistered() {
        int result = 0;
        PreparedStatement pst = null;
        ResultSet rs;
        try {
            pst = con.prepareStatement("SELECT COUNT(*) FROM " + tableName + ";");
            rs = pst.executeQuery();
            if (rs != null && rs.next()) {
                result = rs.getInt(1);
            }
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return result;
        } finally {
            close(pst);
        }
        return result;
    }

    @Override
    public void updateName(String oldone, String newone) {
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnName + "=? WHERE " + columnName + "=?;");
            pst.setString(1, newone);
            pst.setString(2, oldone);
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
        } finally {
            close(pst);
        }
    }

    @Override
    public List<PlayerAuth> getAllAuths() {
        List<PlayerAuth> auths = new ArrayList<>();
        PreparedStatement pst = null;
        ResultSet rs;
        try {
            pst = con.prepareStatement("SELECT * FROM " + tableName + ";");
            rs = pst.executeQuery();
            while (rs.next()) {
                PlayerAuth pAuth;
                if (rs.getString(columnIp).isEmpty()) {
                    pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), "127.0.0.1", rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail), rs.getString(columnRealName));
                } else {
                    if (!columnSalt.isEmpty()) {
                        pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), rs.getString(columnSalt), rs.getInt(columnGroup), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail), rs.getString(columnRealName));
                    } else {
                        pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail), rs.getString(columnRealName));
                    }
                }
                auths.add(pAuth);
            }
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return auths;
        } finally {
            close(pst);
        }
        return auths;
    }

    @Override
    public List<PlayerAuth> getLoggedPlayers() {
        List<PlayerAuth> auths = new ArrayList<>();
        PreparedStatement pst = null;
        ResultSet rs;
        try {
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnLogged + "=1;");
            rs = pst.executeQuery();
            while (rs.next()) {
                PlayerAuth pAuth;
                if (rs.getString(columnIp).isEmpty()) {
                    pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), "127.0.0.1", rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail), rs.getString(columnRealName));
                } else {
                    if (!columnSalt.isEmpty()) {
                        pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), rs.getString(columnSalt), rs.getInt(columnGroup), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail), rs.getString(columnRealName));
                    } else {
                        pAuth = new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getDouble(lastlocX), rs.getDouble(lastlocY), rs.getDouble(lastlocZ), rs.getString(lastlocWorld), rs.getString(columnEmail), rs.getString(columnRealName));
                    }
                }
                auths.add(pAuth);
            }
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return auths;
        } finally {
            close(pst);
        }
        return auths;
    }
}
