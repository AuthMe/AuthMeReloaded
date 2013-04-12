/*
 * Copyright 2011 Sebastian Köhler <sebkoehler@whoami.org.uk>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.whoami.authme.datasource;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import uk.org.whoami.authme.ConsoleLogger;
import uk.org.whoami.authme.cache.auth.PlayerAuth;
import uk.org.whoami.authme.datasource.MiniConnectionPoolManager.TimeoutException;
import uk.org.whoami.authme.settings.Settings;

public class MySQLDataSource implements DataSource {

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
    private String columnEmail;
    private String columnID;
    private List<String> columnOthers;
    private MiniConnectionPoolManager conPool;

    public MySQLDataSource() throws ClassNotFoundException, SQLException {
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
        this.columnSalt = Settings.getMySQLColumnSalt;
        this.columnGroup = Settings.getMySQLColumnGroup;
        this.columnEmail = Settings.getMySQLColumnEmail;
        this.columnOthers = Settings.getMySQLOtherUsernameColumn;
        this.columnID = Settings.getMySQLColumnId;

        connect();
        setup();
    }

    private synchronized void connect() throws ClassNotFoundException, SQLException {
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
            con = conPool.getValidConnection();
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
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + lastlocX + " smallint(6) NOT NULL DEFAULT '0' AFTER "
                        + columnLastLogin +" , ADD " + lastlocY + " smallint(6) NOT NULL DEFAULT '0' AFTER " + lastlocX + " , ADD " + lastlocZ + " smallint(6) NOT NULL DEFAULT '0' AFTER " + lastlocY + ";");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, columnEmail);
            if (!rs.next()) {
            	st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnEmail + " VARCHAR(255) DEFAULT 'your@email.com' AFTER " + lastlocZ +";");
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
            con = conPool.getValidConnection();
             pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE "
                    + columnName + "=?;");               
            
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
        try {
            con = conPool.getValidConnection();
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE "
                    + columnName + "=?;");
            pst.setString(1, user);
            rs = pst.executeQuery();
            if (rs.next()) {
                if (rs.getString(columnIp).isEmpty() ) {
                    return new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), "198.18.0.1", rs.getLong(columnLastLogin), rs.getInt(lastlocX), rs.getInt(lastlocY), rs.getInt(lastlocZ), rs.getString(columnEmail));
                } else {
                        if(!columnSalt.isEmpty()){
                            if(!columnGroup.isEmpty())
                            return new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword),rs.getString(columnSalt), rs.getInt(columnGroup), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getInt(lastlocX), rs.getInt(lastlocY), rs.getInt(lastlocZ), rs.getString(columnEmail));
                            else return new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword),rs.getString(columnSalt), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getInt(lastlocX), rs.getInt(lastlocY), rs.getInt(lastlocZ), rs.getString(columnEmail));
                        } else {
                            return new PlayerAuth(rs.getString(columnName), rs.getString(columnPassword), rs.getString(columnIp), rs.getLong(columnLastLogin), rs.getInt(lastlocX), rs.getInt(lastlocY), rs.getInt(lastlocZ), rs.getString(columnEmail));
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
    }

    @Override
    public synchronized boolean saveAuth(PlayerAuth auth) {
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = conPool.getValidConnection();
            if ((columnSalt.isEmpty() || columnSalt == null) && (auth.getSalt().isEmpty() || auth.getSalt() == null)) {
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
            	for(String column : columnOthers) {
            		pst = con.prepareStatement("UPDATE " + tableName + " SET " + tableName + "." + column + "=? WHERE " + columnName + "=?;");
                    pst.setString(1, auth.getNickname());
                    pst.setString(2, auth.getNickname());
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
            con = conPool.getValidConnection();
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + columnPassword + "=? WHERE " + columnName + "=?;");
            pst.setString(1, auth.getHash());
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
    public boolean updateSession(PlayerAuth auth) {
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = conPool.getValidConnection();
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
    public int purgeDatabase(long until) {
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = conPool.getValidConnection();
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
    public synchronized boolean removeAuth(String user) {
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = conPool.getValidConnection();
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
    public boolean updateQuitLoc(PlayerAuth auth) {
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = conPool.getValidConnection();
            pst = con.prepareStatement("UPDATE " + tableName + " SET "+ lastlocX + " =?, "+ lastlocY +"=?, "+ lastlocZ +"=? WHERE " + columnName + "=?;");
            pst.setLong(1, auth.getQuitLocX());
            pst.setLong(2, auth.getQuitLocY());
            pst.setLong(3, auth.getQuitLocZ());
            pst.setString(4, auth.getNickname());
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
    public int getIps(String ip) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        int countIp=0;
        try {
            con = conPool.getValidConnection();
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
    public boolean updateEmail(PlayerAuth auth) {
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = conPool.getValidConnection();
            pst = con.prepareStatement("UPDATE " + tableName + " SET "+ columnEmail + " =? WHERE " + columnName + "=?;");
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
	public boolean updateSalt(PlayerAuth auth) {
		if (columnSalt.isEmpty()) {
			return false;
		}
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = conPool.getValidConnection();
            pst = con.prepareStatement("UPDATE " + tableName + " SET "+ columnSalt + " =? WHERE " + columnName + "=?;");
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
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        List<String> countIp = new ArrayList<String>();
        try {
            con = conPool.getValidConnection();
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
        } finally {
            close(rs);
            close(pst);
            close(con);
        }     
	}

	@Override
	public List<String> getAllAuthsByIp(String ip) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        List<String> countIp = new ArrayList<String>();
        try {
            con = conPool.getValidConnection();
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
        } finally {
            close(rs);
            close(pst);
            close(con);
        }    
	}

	@Override
	public List<String> getAllAuthsByEmail(String email) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        List<String> countEmail = new ArrayList<String>();
        try {
            con = conPool.getValidConnection();
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
        } finally {
            close(rs);
            close(pst);
            close(con);
        }    
	}

	@Override
	public void purgeBanned(List<String> banned) {
        Connection con = null;
        PreparedStatement pst = null;
        try {
           for (String name : banned) {
        	   con = conPool.getValidConnection();
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

}
