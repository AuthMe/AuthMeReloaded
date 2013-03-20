/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.org.whoami.authme.datasource;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.sqlite.*;
import uk.org.whoami.authme.ConsoleLogger;
import uk.org.whoami.authme.cache.auth.PlayerAuth;
import uk.org.whoami.authme.datasource.MiniConnectionPoolManager.TimeoutException;
import uk.org.whoami.authme.settings.Settings;

/**
 *
 * @author stefano
 */
@SuppressWarnings("unused")
public class SqliteDataSource implements DataSource {

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
    private int nonActivatedGroup;
    private String lastlocX;
    private String lastlocY;
    private String lastlocZ;
    private String columnEmail;
    private String columnID;
    private Connection con;

    public SqliteDataSource() throws ClassNotFoundException, SQLException {
        //Settings s = Settings.getInstance();
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
        this.columnSalt = Settings.getMySQLColumnSalt;
        this.columnGroup = Settings.getMySQLColumnGroup;
        this.lastlocX = Settings.getMySQLlastlocX;
        this.lastlocY = Settings.getMySQLlastlocY;
        this.lastlocZ = Settings.getMySQLlastlocZ;
        this.nonActivatedGroup = Settings.getNonActivatedGroup;
        this.columnEmail = Settings.getMySQLColumnEmail;
        this.columnID = Settings.getMySQLColumnId;
        
        connect();
        setup();
    }

    private synchronized void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        ConsoleLogger.info("SQLite driver loaded");

        this.con = DriverManager.getConnection("jdbc:sqlite:plugins/AuthMe/"+database+".db");

    }

    private synchronized void setup() throws SQLException {
        //Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            st = con.createStatement();
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + tableName + " ("
                    + columnID + " INTEGER AUTO_INCREMENT,"
                    + columnName + " VARCHAR(255) NOT NULL UNIQUE,"
                    + columnPassword + " VARCHAR(255) NOT NULL,"
                    + columnIp + " VARCHAR(40) NOT NULL,"
                    + columnLastLogin + " BIGINT,"
                    + lastlocX + " smallint(6) DEFAULT '0',"
                    + lastlocY + " smallint(6) DEFAULT '0',"
                    + lastlocZ + " smallint(6) DEFAULT '0',"
                    + columnEmail + " VARCHAR(255) DEFAULT 'your@email.com',"
                    + "CONSTRAINT table_const_prim PRIMARY KEY (" + columnID + "));");
            
            rs = con.getMetaData().getColumns(null, null, tableName, columnPassword);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                        + columnPassword + " VARCHAR(255) NOT NULL;");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, columnIp);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                        + columnIp + " VARCHAR(40) NOT NULL;");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, columnLastLogin);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                        + columnLastLogin + " BIGINT;");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, lastlocX);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + lastlocX + " smallint(6) NOT NULL DEFAULT '0'; " 
                        + "ALTER TABLE " + tableName + " ADD COLUMN " + lastlocY + " smallint(6) NOT NULL DEFAULT '0'; "
                        + "ALTER TABLE " + tableName + " ADD COLUMN " + lastlocZ + " smallint(6) NOT NULL DEFAULT '0';");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, columnEmail);
            if (!rs.next()) {
            	st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnEmail + "  VARCHAR(255) DEFAULT 'your@email.com';");
            }
        } finally {
            close(rs);
            close(st);
            //close(con);
        }
        ConsoleLogger.info("SQLite Setup finished");
    }

    @Override
    public synchronized boolean isAuthAvailable(String user) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + columnName + "=?");
            pst.setString(1, user);
            rs = pst.executeQuery();
            return rs.next();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(rs);
            close(pst);
            //close(con);
        }
    }

    @Override
    public synchronized PlayerAuth getAuth(String user) {
        //Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
           
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE "
                    + columnName + "=?;");
            pst.setString(1, user);
            rs = pst.executeQuery();
            if (rs.next()) {
                if (rs.getString(columnIp).isEmpty() ) {
                    //System.out.println("[Authme Debug] ColumnIp is empty");
                    return new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), "198.18.0.1", rs.getLong(columnLastLogin), rs.getInt(lastlocX), rs.getInt(lastlocY), rs.getInt(lastlocZ), rs.getString(columnEmail));
                } else {
                        if(!columnSalt.isEmpty()){
                            //System.out.println("[Authme Debug] column Salt is" + rs.getString(columnSalt));
                            return new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword),rs.getString(columnSalt), rs.getInt(columnGroup), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getInt(lastlocX), rs.getInt(lastlocY), rs.getInt(lastlocZ), rs.getString(columnEmail));
                        } else {
                    //System.out.println("[Authme Debug] column Salt is empty");
                            return new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getInt(lastlocX), rs.getInt(lastlocY), rs.getInt(lastlocZ), rs.getString(columnEmail));
                       
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
            //close(con);
        }
    }

    @Override
    public synchronized boolean saveAuth(PlayerAuth auth) {
        //Connection con = null;
        PreparedStatement pst = null;
        try {
            if (columnSalt.isEmpty() && auth.getSalt().isEmpty()) {
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
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(pst);
            //close(con);
        }
        return true;
    }

    @Override
    public synchronized boolean updatePassword(PlayerAuth auth) {
        //Connection con = null;
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
            //close(con);
        }
        return true;
    }

    @Override
    public boolean updateSession(PlayerAuth auth) {
        //Connection con = null;
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnIp + "=?, " + columnLastLogin + "=? WHERE " + columnName + "=?;");
            pst.setString(1, auth.getIp());
            pst.setLong(2, auth.getLastLogin());
            pst.setString(3, auth.getNickname());
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(pst);
            //close(con);
        }
        return true;
    }

    @Override
    public int purgeDatabase(long until) {
        //Connection con = null;
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
            //close(con);
        }
    }

    @Override
    public synchronized boolean removeAuth(String user) {
        //Connection con = null;
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
            //close(con);
        }
        return true;
    }
    
    @Override
    public boolean updateQuitLoc(PlayerAuth auth) {
        //Connection con = null;
        PreparedStatement pst = null;
        try {
           
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + lastlocX + "=?, "+ lastlocY +"=?, "+ lastlocZ +"=? WHERE " + columnName + "=?;");
            pst.setLong(1, auth.getQuitLocX());
            pst.setLong(2, auth.getQuitLocY());
            pst.setLong(3, auth.getQuitLocZ());
            pst.setString(4, auth.getNickname());
            pst.executeUpdate();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            close(pst);
            //close(con);
        }
        return true;
    }
    
    //
    // Check how many registration by given ip has been done
    //
    
    @Override
    public int getIps(String ip) {
        //Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        int countIp=0;
        try {
            
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE "
                    + columnIp + "=?;");
            pst.setString(1, ip);
            rs = pst.executeQuery();
            while(rs.next()) {
                countIp++;    
            } 
             return countIp;
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return 0;
        }  finally {
            close(rs);
            close(pst);
            //close(con);
        }         
    }
    
	@Override
	public boolean updateEmail(PlayerAuth auth) {
        //Connection con = null;
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
            //close(con);
        }
        return true;
    }
	
	@Override
	public boolean updateSalt(PlayerAuth auth) {
		if(columnSalt.isEmpty()) {
			return false;
		}
        //Connection con = null;
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
            //close(con);
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
	public List<String> getAllAuthsByName(PlayerAuth auth) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        List<String> countIp = new ArrayList<String>();
        try {
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE "
                    + columnIp + "=?;");
            pst.setString(1, auth.getIp());
            rs = pst.executeQuery();
            while(rs.next()) {
                countIp.add(rs.getString(columnName));    
            } 
             return countIp;
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<String>();
        } catch (TimeoutException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<String>();
        } catch (NullPointerException npe) {
        	return new ArrayList<String>();
        } finally {
            close(rs);
            close(pst);
            //close(con);
        } 
	}

	@Override
	public List<String> getAllAuthsByIp(String ip) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        List<String> countIp = new ArrayList<String>();
        try {
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE "
                    + columnIp + "=?;");
            pst.setString(1, ip);
            rs = pst.executeQuery();
            while(rs.next()) {
                countIp.add(rs.getString(columnName));    
            } 
             return countIp;
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<String>();
        } catch (TimeoutException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<String>();
        } catch (NullPointerException npe) {
        	return new ArrayList<String>();
        } finally {
            close(rs);
            close(pst);
            //close(con);
        } 
	}
	
	@Override
	public List<String> getAllAuthsByEmail(String email) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        List<String> countEmail = new ArrayList<String>();
        try {
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE "
                    + columnEmail + "=?;");
            pst.setString(1, email);
            rs = pst.executeQuery();
            while(rs.next()) {
                countEmail.add(rs.getString(columnName));    
            } 
             return countEmail;
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<String>();
        } catch (TimeoutException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<String>();
        } catch (NullPointerException npe) {
        	return new ArrayList<String>();
        } finally {
            close(rs);
            close(pst);
        } 
	}
    
}
