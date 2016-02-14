package fr.xephi.authme.datasource;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.util.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class SQLite implements DataSource {

    private final String database;
    private final String tableName;
    private final Columns col;
    private Connection con;

    /**
     * Constructor for SQLite.
     *
     * @throws ClassNotFoundException Exception
     * @throws SQLException           Exception
     */
    public SQLite(NewSetting settings) throws ClassNotFoundException, SQLException {
        this.database = settings.getProperty(DatabaseSettings.MYSQL_DATABASE);
        this.tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);
        this.col = new Columns(settings);

        try {
            this.connect();
            this.setup();
        } catch (ClassNotFoundException | SQLException ex) {
            ConsoleLogger.logException("Error during SQLite initialization:", ex);
            throw ex;
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
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + tableName + " (" + col.ID + " INTEGER AUTO_INCREMENT," + col.NAME + " VARCHAR(255) NOT NULL UNIQUE," + col.PASSWORD + " VARCHAR(255) NOT NULL," + col.IP + " VARCHAR(40) NOT NULL," + col.LAST_LOGIN + " BIGINT," + col.LASTLOC_X + " DOUBLE NOT NULL DEFAULT '0.0'," + col.LASTLOC_Y + " DOUBLE NOT NULL DEFAULT '0.0'," + col.LASTLOC_Z + " DOUBLE NOT NULL DEFAULT '0.0'," + col.LASTLOC_WORLD + " VARCHAR(255) NOT NULL DEFAULT '" + Settings.defaultWorld + "'," + col.EMAIL + " VARCHAR(255) DEFAULT 'your@email.com'," + "CONSTRAINT table_const_prim PRIMARY KEY (" + col.ID + "));");
            rs = con.getMetaData().getColumns(null, null, tableName, col.PASSWORD);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + col.PASSWORD + " VARCHAR(255) NOT NULL;");
            }
            rs.close();
            if (!col.SALT.isEmpty()) {
                rs = con.getMetaData().getColumns(null, null, tableName, col.SALT);
                if (!rs.next()) {
                    st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + col.SALT + " VARCHAR(255);");
                }
                rs.close();
            }
            rs = con.getMetaData().getColumns(null, null, tableName, col.IP);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + col.IP + " VARCHAR(40) NOT NULL;");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, col.LAST_LOGIN);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + col.LAST_LOGIN + " TIMESTAMP DEFAULT current_timestamp;");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, col.LASTLOC_X);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + col.LASTLOC_X + " DOUBLE NOT NULL DEFAULT '0.0';");
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + col.LASTLOC_Y + " DOUBLE NOT NULL DEFAULT '0.0';");
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + col.LASTLOC_Z + " DOUBLE NOT NULL DEFAULT '0.0';");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, col.LASTLOC_WORLD);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + col.LASTLOC_WORLD + " VARCHAR(255) NOT NULL DEFAULT 'world';");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, col.EMAIL);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + col.EMAIL + " VARCHAR(255) DEFAULT 'your@email.com';");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, col.IS_LOGGED);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + col.IS_LOGGED + " INT DEFAULT '0';");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, col.REAL_NAME);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + col.REAL_NAME + " VARCHAR(255) NOT NULL DEFAULT 'Player';");
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
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE LOWER(" + col.NAME + ")=LOWER(?);");
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
    public HashedPassword getPassword(String user) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = con.prepareStatement("SELECT " + col.PASSWORD + "," + col.SALT
                + " FROM " + tableName + " WHERE " + col.NAME + "=?");
            pst.setString(1, user);
            rs = pst.executeQuery();
            if (rs.next()) {
                return new HashedPassword(rs.getString(col.PASSWORD),
                    !col.SALT.isEmpty() ? rs.getString(col.SALT) : null);
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        } finally {
            close(rs);
            close(pst);
        }
        return null;
    }

    @Override
    public synchronized PlayerAuth getAuth(String user) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE LOWER(" + col.NAME + ")=LOWER(?);");
            pst.setString(1, user);
            rs = pst.executeQuery();
            if (rs.next()) {
                return buildAuthFromResultSet(rs);
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
            HashedPassword password = auth.getPassword();
            if (col.SALT.isEmpty()) {
                if (!StringUtils.isEmpty(auth.getPassword().getSalt())) {
                    ConsoleLogger.showError("Warning! Detected hashed password with separate salt but the salt column "
                        + "is not set in the config!");
                }
                pst = con.prepareStatement("INSERT INTO " + tableName + "(" + col.NAME + "," + col.PASSWORD +
                    "," + col.IP + "," + col.LAST_LOGIN + "," + col.REAL_NAME + "," + col.EMAIL +
                    ") VALUES (?,?,?,?,?,?);");
                pst.setString(1, auth.getNickname());
                pst.setString(2, password.getHash());
                pst.setString(3, auth.getIp());
                pst.setLong(4, auth.getLastLogin());
                pst.setString(5, auth.getRealName());
                pst.setString(6, auth.getEmail());
                pst.executeUpdate();
            } else {
                pst = con.prepareStatement("INSERT INTO " + tableName + "(" + col.NAME + "," + col.PASSWORD + ","
                    + col.IP + "," + col.LAST_LOGIN + "," + col.REAL_NAME + "," + col.EMAIL + "," + col.SALT
                    + ") VALUES (?,?,?,?,?,?,?);");
                pst.setString(1, auth.getNickname());
                pst.setString(2, password.getHash());
                pst.setString(3, auth.getIp());
                pst.setLong(4, auth.getLastLogin());
                pst.setString(5, auth.getRealName());
                pst.setString(6, auth.getEmail());
                pst.setString(7, password.getSalt());
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
        return updatePassword(auth.getNickname(), auth.getPassword());
    }

    @Override
    public boolean updatePassword(String user, HashedPassword password) {
        user = user.toLowerCase();
        PreparedStatement pst = null;
        try {
            boolean useSalt = !col.SALT.isEmpty();
            String sql = "UPDATE " + tableName + " SET " + col.PASSWORD + " = ?"
                + (useSalt ? ", " + col.SALT + " = ?" : "")
                + " WHERE " + col.NAME + " = ?";
            pst = con.prepareStatement(sql);
            pst.setString(1, password.getHash());
            if (useSalt) {
                pst.setString(2, password.getSalt());
                pst.setString(3, user);
            } else {
                pst.setString(2, user);
            }
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
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + col.IP + "=?, " + col.LAST_LOGIN + "=?, " + col.REAL_NAME + "=? WHERE " + col.NAME + "=?;");
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

            pst = con.prepareStatement("DELETE FROM " + tableName + " WHERE " + col.LAST_LOGIN + "<?;");
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
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + col.LAST_LOGIN + "<?;");
            pst.setLong(1, until);
            rs = pst.executeQuery();
            while (rs.next()) {
                list.add(rs.getString(col.NAME));
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
            pst = con.prepareStatement("DELETE FROM " + tableName + " WHERE " + col.NAME + "=?;");
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
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + col.LASTLOC_X + "=?, " + col.LASTLOC_Y + "=?, " + col.LASTLOC_Z + "=?, " + col.LASTLOC_WORLD + "=? WHERE " + col.NAME + "=?;");
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
            // TODO ljacqu 20151230: Simply fetch COUNT(1) and return that
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + col.IP + "=?;");
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
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + col.EMAIL + "=? WHERE " + col.NAME + "=?;");
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
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + col.IP + "=?;");
            pst.setString(1, auth.getIp());
            rs = pst.executeQuery();
            while (rs.next()) {
                countIp.add(rs.getString(col.NAME));
            }
            return countIp;
        } catch (SQLException ex) {
            logSqlException(ex);
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
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + col.IP + "=?;");
            pst.setString(1, ip);
            rs = pst.executeQuery();
            while (rs.next()) {
                countIp.add(rs.getString(col.NAME));
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
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + col.EMAIL + "=?;");
            pst.setString(1, email);
            rs = pst.executeQuery();
            while (rs.next()) {
                countEmail.add(rs.getString(col.NAME));
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
                pst = con.prepareStatement("DELETE FROM " + tableName + " WHERE " + col.NAME + "=?;");
                pst.setString(1, name);
                pst.executeUpdate();
            }
        } catch (SQLException ex) {
            logSqlException(ex);
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
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE LOWER(" + col.NAME + ")=?;");
            pst.setString(1, user);
            rs = pst.executeQuery();
            if (rs.next())
                return (rs.getInt(col.IS_LOGGED) == 1);
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
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + col.IS_LOGGED + "=? WHERE LOWER(" + col.NAME + ")=?;");
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
                pst = con.prepareStatement("UPDATE " + tableName + " SET " + col.IS_LOGGED + "=? WHERE LOWER(" + col.NAME + ")=?;");
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
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + col.IS_LOGGED + "=? WHERE " + col.IS_LOGGED + "=?;");
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
    public void updateName(String oldOne, String newOne) {
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("UPDATE " + tableName + " SET " + col.NAME + "=? WHERE " + col.NAME + "=?;");
            pst.setString(1, newOne);
            pst.setString(2, oldOne);
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
                PlayerAuth auth = buildAuthFromResultSet(rs);
                auths.add(auth);
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
            pst = con.prepareStatement("SELECT * FROM " + tableName + " WHERE " + col.IS_LOGGED + "=1;");
            rs = pst.executeQuery();
            while (rs.next()) {
                PlayerAuth auth = buildAuthFromResultSet(rs);
                auths.add(auth);
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
    public synchronized boolean isEmailStored(String email) {
        String sql = "SELECT 1 FROM " + tableName + " WHERE " + col.EMAIL + " = ? COLLATE NOCASE;";
        ResultSet rs = null;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logSqlException(e);
        } finally {
            close(rs);
        }
        return false;
    }

    private static void logSqlException(SQLException e) {
        ConsoleLogger.showError("Error while executing SQL statement: " + StringUtils.formatException(e));
        ConsoleLogger.writeStackTrace(e);
    }

    private PlayerAuth buildAuthFromResultSet(ResultSet row) throws SQLException {
        String salt = !col.SALT.isEmpty() ? row.getString(col.SALT) : null;

        PlayerAuth.Builder authBuilder = PlayerAuth.builder()
            .name(row.getString(col.NAME))
            .email(row.getString(col.EMAIL))
            .realName(row.getString(col.REAL_NAME))
            .password(row.getString(col.PASSWORD), salt)
            .lastLogin(row.getLong(col.LAST_LOGIN))
            .locX(row.getDouble(col.LASTLOC_X))
            .locY(row.getDouble(col.LASTLOC_Y))
            .locZ(row.getDouble(col.LASTLOC_Z))
            .locWorld(row.getString(col.LASTLOC_WORLD));

        String ip = row.getString(col.IP);
        if (!ip.isEmpty()) {
            authBuilder.ip(ip);
        }
        return authBuilder.build();
    }
}
