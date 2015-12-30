package fr.xephi.authme.datasource;

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
public class SQLite implements DataSource {

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
    private Connection con;
    private final String columnLogged;
    private final String columnRealName;

    /**
     * Constructor for SQLite.
     *
     * @throws ClassNotFoundException * @throws SQLException
     */
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

    /**
     * Method connect.
     *
     * @throws ClassNotFoundException * @throws SQLException
     */
    private synchronized void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        ConsoleLogger.info("SQLite driver loaded");
        this.con = DriverManager.getConnection("jdbc:sqlite:plugins/AuthMe/" + database + ".db");

    }

    private synchronized void reconnect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        this.con = DriverManager.getConnection("jdbc:sqlite:plugins/AuthMe/" + database + ".db");
    }

    @Override
    public synchronized Connection getConnection() throws SQLException
    {
    	if (this.con.isClosed())
			try {
				reconnect();
			} catch (ClassNotFoundException e) {
				ConsoleLogger.writeStackTrace(e);
			}
    	return this.con;
    }

    /**
     * Method setup.
     *
     * @throws SQLException
     */
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

    /**
     * Method isAuthAvailable.
     *
     * @param user String
     *
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#isAuthAvailable(String)
     */
    @Override
    public synchronized boolean isAuthAvailable(String user) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = getConnection().prepareStatement(new Query()
            		.select("*")
            		.from(tableName)
            		.addWhere("LOWER(" + columnName + ")=LOWER(?)", null)
            		.build()
            		.getQuery());
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

    /**
     * Method getAuth.
     *
     * @param user String
     *
     * @return PlayerAuth * @see fr.xephi.authme.datasource.DataSource#getAuth(String)
     */
    @Override
    public synchronized PlayerAuth getAuth(String user) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = getConnection().prepareStatement(new Query()
            		.select("*")
            		.from(tableName)
            		.addWhere("LOWER(" + columnName + ")=LOWER(?)", null)
            		.build()
            		.getQuery());
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

    /**
     * Method saveAuth.
     *
     * @param auth PlayerAuth
     *
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#saveAuth(PlayerAuth)
     */
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

    /**
     * Method updatePassword.
     *
     * @param auth PlayerAuth
     *
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#updatePassword(PlayerAuth)
     */
    @Override
    public synchronized boolean updatePassword(PlayerAuth auth) {
        try {
        	PreparedStatement pst = getConnection().prepareStatement(new Query()
         			.update()
         			.from(tableName)
         			.addUpdateSet(columnPassword + "=?")
         			.addWhere(columnName + "=?", null)
         			.build()
         			.getQuery());
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnPassword + "=? WHERE " + columnName + "=?;");
            pst.setString(1, auth.getHash());
            pst.setString(2, auth.getNickname());
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        }
        return true;
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
        try {
        	PreparedStatement pst = getConnection().prepareStatement(new Query()
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
        try {
        	PreparedStatement pst = getConnection().prepareStatement(new Query()
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
        try {
        	PreparedStatement st = getConnection().prepareStatement(new Query()
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
            st = getConnection().prepareStatement(new Query()
            		.delete()
            		.from(tableName)
            		.addWhere(columnLastLogin + "<" + until, null)
            		.build()
            		.getQuery());
            st.executeUpdate();
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
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#removeAuth(String)
     */
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
        try {
        	PreparedStatement pst = getConnection().prepareStatement(new Query()
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
        try {
        	PreparedStatement pst = getConnection().prepareStatement(new Query()
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
        try {
            PreparedStatement pst = getConnection().prepareStatement(new Query()
            		.update()
            		.from(tableName)
            		.addUpdateSet(columnEmail + "=?")
            		.addWhere(columnName + "=?", null)
            		.build()
            		.getQuery());
            pst.setString(1, auth.getEmail());
            pst.setString(2, auth.getNickname());
            pst.executeUpdate();
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
        try {
            PreparedStatement pst = getConnection().prepareStatement(new Query()
            		.update()
            		.from(tableName)
            		.addUpdateSet(columnSalt + "=?")
            		.addWhere(columnName + "=?", null)
            		.build()
            		.getQuery());
            pst.setString(1, auth.getSalt());
            pst.setString(2, auth.getNickname());
            pst.executeUpdate();
            return true;
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
        return false;
    }

    /**
     * Method close.
     *
     * @see fr.xephi.authme.datasource.DataSource#close()
     */
    @Override
    public synchronized void close() {
        try {
            con.close();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
        }
    }

    /**
     * Method reload.
     *
     * @see fr.xephi.authme.datasource.DataSource#reload()
     */
    @Override
    public void reload() {
    }

    /**
     * Method close.
     *
     * @param st Statement
     */
    private void close(Statement st) {
        if (st != null) {
            try {
                st.close();
            } catch (SQLException ex) {
                ConsoleLogger.showError(ex.getMessage());
            }
        }
    }

    /**
     * Method close.
     *
     * @param rs ResultSet
     */
    private void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ex) {
                ConsoleLogger.showError(ex.getMessage());
            }
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
        try {
        	PreparedStatement pst = getConnection().prepareStatement(new Query()
        			.delete()
        			.from(tableName)
        			.addWhere(columnName + "=?", null)
        			.build()
        			.getQuery());
            for (String name : banned) {
                pst.setString(1, name);
                pst.executeUpdate();
            }
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
        return DataSourceType.SQLITE;
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
        try {
            PreparedStatement pst = getConnection().prepareStatement(new Query()
            		.update()
            		.from(tableName)
            		.addUpdateSet(columnLogged + "='0'")
            		.addWhere(columnLogged + "='1'", null)
            		.build()
            		.getQuery());
            pst.executeUpdate();
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
        try {
        	PreparedStatement st = getConnection().prepareStatement(new Query()
        			.select("COUNT(*)")
        			.from(tableName)
        			.build()
        			.getQuery());
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                result = rs.getInt(1);
            }
            rs.close();
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
        try {
        	PreparedStatement st = getConnection().prepareStatement(new Query()
            		.select("*")
            		.from(tableName)
            		.build()
            		.getQuery());
            ResultSet rs = st
            		.executeQuery();
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
                auths.add(pAuth);
            }
            rs.close();
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.writeStackTrace(ex);
        }
        return auths;
    }

    /**
     * Method getLoggedPlayers.
     *
     * @return List<PlayerAuth> * @see fr.xephi.authme.datasource.DataSource#getLoggedPlayers()
     */
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
