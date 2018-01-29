package fr.xephi.authme.datasource;

import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.util.StringUtils;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fr.xephi.authme.datasource.SqlDataSourceUtils.getNullableLong;
import static fr.xephi.authme.datasource.SqlDataSourceUtils.logSqlException;

/**
 * SQLite data source.
 */
public class SQLite implements DataSource {

    private final Settings settings;
    private final File dataFolder;
    private final String database;
    private final String tableName;
    private final Columns col;
    private Connection con;

    /**
     * Constructor for SQLite.
     *
     * @param settings The settings instance
     * @param dataFolder The data folder
     *
     * @throws SQLException when initialization of a SQL datasource failed
     */
    public SQLite(Settings settings, File dataFolder) throws SQLException {
        this.settings = settings;
        this.dataFolder = dataFolder;
        this.database = settings.getProperty(DatabaseSettings.MYSQL_DATABASE);
        this.tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);
        this.col = new Columns(settings);

        try {
            this.connect();
            this.setup();
            this.migrateIfNeeded();
        } catch (Exception ex) {
            ConsoleLogger.logException("Error during SQLite initialization:", ex);
            throw ex;
        }
    }

    @VisibleForTesting
    SQLite(Settings settings, File dataFolder, Connection connection) {
        this.settings = settings;
        this.dataFolder = dataFolder;
        this.database = settings.getProperty(DatabaseSettings.MYSQL_DATABASE);
        this.tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);
        this.col = new Columns(settings);
        this.con = connection;
    }

    /**
     * Initializes the connection to the SQLite database.
     */
    protected void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Failed to load SQLite JDBC class", e);
        }

        ConsoleLogger.debug("SQLite driver loaded");
        this.con = DriverManager.getConnection("jdbc:sqlite:plugins/AuthMe/" + database + ".db");
    }

    /**
     * Creates the table if necessary, or adds any missing columns to the table.
     */
    @VisibleForTesting
    protected void setup() throws SQLException {
        try (Statement st = con.createStatement()) {
            // Note: cannot add unique fields later on in SQLite, so we add it on initialization
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + col.ID + " INTEGER AUTO_INCREMENT, "
                + col.NAME + " VARCHAR(255) NOT NULL UNIQUE, "
                + "CONSTRAINT table_const_prim PRIMARY KEY (" + col.ID + "));");

            DatabaseMetaData md = con.getMetaData();

            if (isColumnMissing(md, col.REAL_NAME)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.REAL_NAME + " VARCHAR(255) NOT NULL DEFAULT 'Player';");
            }

            if (isColumnMissing(md, col.PASSWORD)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.PASSWORD + " VARCHAR(255) NOT NULL DEFAULT '';");
            }

            if (!col.SALT.isEmpty() && isColumnMissing(md, col.SALT)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + col.SALT + " VARCHAR(255);");
            }

            if (isColumnMissing(md, col.LAST_IP)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.LAST_IP + " VARCHAR(40);");
            }

            if (isColumnMissing(md, col.LAST_LOGIN)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.LAST_LOGIN + " TIMESTAMP;");
            }

            if (isColumnMissing(md, col.REGISTRATION_IP)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.REGISTRATION_IP + " VARCHAR(40);");
            }

            if (isColumnMissing(md, col.REGISTRATION_DATE)) {
                addRegistrationDateColumn(st);
            }

            if (isColumnMissing(md, col.LASTLOC_X)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + col.LASTLOC_X
                    + " DOUBLE NOT NULL DEFAULT '0.0';");
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + col.LASTLOC_Y
                    + " DOUBLE NOT NULL DEFAULT '0.0';");
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + col.LASTLOC_Z
                    + " DOUBLE NOT NULL DEFAULT '0.0';");
            }

            if (isColumnMissing(md, col.LASTLOC_WORLD)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.LASTLOC_WORLD + " VARCHAR(255) NOT NULL DEFAULT 'world';");
            }

            if (isColumnMissing(md, col.LASTLOC_YAW)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.LASTLOC_YAW + " FLOAT;");
            }

            if (isColumnMissing(md, col.LASTLOC_PITCH)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.LASTLOC_PITCH + " FLOAT;");
            }

            if (isColumnMissing(md, col.EMAIL)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.EMAIL + " VARCHAR(255);");
            }

            if (isColumnMissing(md, col.IS_LOGGED)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.IS_LOGGED + " INT NOT NULL DEFAULT '0';");
            }

            if (isColumnMissing(md, col.HAS_SESSION)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.HAS_SESSION + " INT NOT NULL DEFAULT '0';");
            }
        }
        ConsoleLogger.info("SQLite Setup finished");
    }

    protected void migrateIfNeeded() throws SQLException {
        DatabaseMetaData metaData = con.getMetaData();
        if (SqLiteMigrater.isMigrationRequired(metaData, tableName, col)) {
            new SqLiteMigrater(settings, dataFolder).performMigration(this);
            // Migration deletes the table and recreates it, therefore call connect() again
            // to get an up-to-date Connection to the database
            connect();
        }
    }

    private boolean isColumnMissing(DatabaseMetaData metaData, String columnName) throws SQLException {
        try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
            return !rs.next();
        }
    }

    @Override
    public void reload() {
        close(con);
        try {
            this.connect();
            this.setup();
            this.migrateIfNeeded();
        } catch (SQLException ex) {
            ConsoleLogger.logException("Error while reloading SQLite:", ex);
        }
    }

    @Override
    public boolean isAuthAvailable(String user) {
        String sql = "SELECT 1 FROM " + tableName + " WHERE LOWER(" + col.NAME + ")=LOWER(?);";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            ConsoleLogger.warning(ex.getMessage());
            return false;
        }
    }

    @Override
    public HashedPassword getPassword(String user) {
        boolean useSalt = !col.SALT.isEmpty();
        String sql = "SELECT " + col.PASSWORD
            + (useSalt ? ", " + col.SALT : "")
            + " FROM " + tableName + " WHERE " + col.NAME + "=?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user);
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
        String sql = "SELECT * FROM " + tableName + " WHERE LOWER(" + col.NAME + ")=LOWER(?);";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return buildAuthFromResultSet(rs);
                }
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return null;
    }

    @Override
    public boolean saveAuth(PlayerAuth auth) {
        PreparedStatement pst = null;
        try {
            HashedPassword password = auth.getPassword();
            if (col.SALT.isEmpty()) {
                if (!StringUtils.isEmpty(auth.getPassword().getSalt())) {
                    ConsoleLogger.warning("Warning! Detected hashed password with separate salt but the salt column "
                        + "is not set in the config!");
                }

                pst = con.prepareStatement("INSERT INTO " + tableName + "(" + col.NAME + "," + col.PASSWORD
                    + "," + col.REAL_NAME + "," + col.EMAIL
                    + "," + col.REGISTRATION_DATE + "," + col.REGISTRATION_IP
                    + ") VALUES (?,?,?,?,?,?);");
                pst.setString(1, auth.getNickname());
                pst.setString(2, password.getHash());
                pst.setString(3, auth.getRealName());
                pst.setString(4, auth.getEmail());
                pst.setLong(5, auth.getRegistrationDate());
                pst.setString(6, auth.getRegistrationIp());
                pst.executeUpdate();
            } else {
                pst = con.prepareStatement("INSERT INTO " + tableName + "(" + col.NAME + "," + col.PASSWORD
                    + "," + col.REAL_NAME + "," + col.EMAIL
                    + "," + col.REGISTRATION_DATE + "," + col.REGISTRATION_IP + "," + col.SALT
                    + ") VALUES (?,?,?,?,?,?,?);");
                pst.setString(1, auth.getNickname());
                pst.setString(2, password.getHash());
                pst.setString(3, auth.getRealName());
                pst.setString(4, auth.getEmail());
                pst.setLong(5, auth.getRegistrationDate());
                pst.setString(6, auth.getRegistrationIp());
                pst.setString(7, password.getSalt());
                pst.executeUpdate();
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        } finally {
            close(pst);
        }
        return true;
    }

    @Override
    public boolean updatePassword(PlayerAuth auth) {
        return updatePassword(auth.getNickname(), auth.getPassword());
    }

    @Override
    public boolean updatePassword(String user, HashedPassword password) {
        user = user.toLowerCase();
        boolean useSalt = !col.SALT.isEmpty();
        String sql = "UPDATE " + tableName + " SET " + col.PASSWORD + " = ?"
            + (useSalt ? ", " + col.SALT + " = ?" : "")
            + " WHERE " + col.NAME + " = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)){
            pst.setString(1, password.getHash());
            if (useSalt) {
                pst.setString(2, password.getSalt());
                pst.setString(3, user);
            } else {
                pst.setString(2, user);
            }
            pst.executeUpdate();
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public boolean updateSession(PlayerAuth auth) {
        String sql = "UPDATE " + tableName + " SET " + col.LAST_IP + "=?, " + col.LAST_LOGIN + "=?, "
            + col.REAL_NAME + "=? WHERE " + col.NAME + "=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)){
            pst.setString(1, auth.getLastIp());
            pst.setObject(2, auth.getLastLogin());
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
    public Set<String> getRecordsToPurge(long until) {
        Set<String> list = new HashSet<>();
        String select = "SELECT " + col.NAME + " FROM " + tableName + " WHERE MAX("
            + " COALESCE(" + col.LAST_LOGIN + ", 0),"
            + " COALESCE(" + col.REGISTRATION_DATE + ", 0)"
            + ") < ?;";
        try (PreparedStatement selectPst = con.prepareStatement(select)) {
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
    public void purgeRecords(Collection<String> toPurge) {
        String delete = "DELETE FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (PreparedStatement deletePst = con.prepareStatement(delete)) {
            for (String name : toPurge) {
                deletePst.setString(1, name.toLowerCase());
                deletePst.executeUpdate();
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
    }

    @Override
    public boolean removeAuth(String user) {
        String sql = "DELETE FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user.toLowerCase());
            pst.executeUpdate();
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public boolean updateQuitLoc(PlayerAuth auth) {
        String sql = "UPDATE " + tableName + " SET "
            + col.LASTLOC_X + "=?, " + col.LASTLOC_Y + "=?, " + col.LASTLOC_Z + "=?, "
            + col.LASTLOC_WORLD + "=?, " + col.LASTLOC_YAW + "=?, " + col.LASTLOC_PITCH + "=? "
            + "WHERE " + col.NAME + "=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setDouble(1, auth.getQuitLocX());
            pst.setDouble(2, auth.getQuitLocY());
            pst.setDouble(3, auth.getQuitLocZ());
            pst.setString(4, auth.getWorld());
            pst.setFloat(5, auth.getYaw());
            pst.setFloat(6, auth.getPitch());
            pst.setString(7, auth.getNickname());
            pst.executeUpdate();
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public boolean updateEmail(PlayerAuth auth) {
        String sql = "UPDATE " + tableName + " SET " + col.EMAIL + "=? WHERE " + col.NAME + "=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
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
    public void closeConnection() {
        try {
            if (con != null && !con.isClosed()) {
                con.close();
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
    }

    @Override
    public List<String> getAllAuthsByIp(String ip) {
        List<String> countIp = new ArrayList<>();
        String sql = "SELECT " + col.NAME + " FROM " + tableName + " WHERE " + col.LAST_IP + "=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, ip);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    countIp.add(rs.getString(col.NAME));
                }
                return countIp;
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return new ArrayList<>();
    }

    @Override
    public int countAuthsByEmail(String email) {
        String sql = "SELECT COUNT(1) FROM " + tableName + " WHERE " + col.EMAIL + " = ? COLLATE NOCASE;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
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
    public DataSourceType getType() {
        return DataSourceType.SQLITE;
    }

    @Override
    public boolean isLogged(String user) {
        String sql = "SELECT " + col.IS_LOGGED + " FROM " + tableName + " WHERE LOWER(" + col.NAME + ")=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(col.IS_LOGGED) == 1;
                }
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public void setLogged(String user) {
        String sql = "UPDATE " + tableName + " SET " + col.IS_LOGGED + "=? WHERE LOWER(" + col.NAME + ")=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, 1);
            pst.setString(2, user);
            pst.executeUpdate();
        } catch (SQLException ex) {
            logSqlException(ex);
        }
    }

    @Override
    public void setUnlogged(String user) {
        String sql = "UPDATE " + tableName + " SET " + col.IS_LOGGED + "=? WHERE LOWER(" + col.NAME + ")=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, 0);
            pst.setString(2, user);
            pst.executeUpdate();
        } catch (SQLException ex) {
            logSqlException(ex);
        }
    }

    @Override
    public boolean hasSession(String user) {
        String sql = "SELECT " + col.HAS_SESSION + " FROM " + tableName + " WHERE LOWER(" + col.NAME + ")=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user.toLowerCase());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(col.HAS_SESSION) == 1;
                }
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public void grantSession(String user) {
        String sql = "UPDATE " + tableName + " SET " + col.HAS_SESSION + "=? WHERE LOWER(" + col.NAME + ")=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, 1);
            pst.setString(2, user.toLowerCase());
            pst.executeUpdate();
        } catch (SQLException ex) {
            logSqlException(ex);
        }
    }

    @Override
    public void revokeSession(String user) {
        String sql = "UPDATE " + tableName + " SET " + col.HAS_SESSION + "=? WHERE LOWER(" + col.NAME + ")=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
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
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, 0);
            pst.setInt(2, 1);
            pst.executeUpdate();
        } catch (SQLException ex) {
            logSqlException(ex);
        }
    }

    @Override
    public int getAccountsRegistered() {
        String sql = "SELECT COUNT(*) FROM " + tableName + ";";
        try (PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return 0;
    }

    @Override
    public boolean updateRealName(String user, String realName) {
        String sql = "UPDATE " + tableName + " SET " + col.REAL_NAME + "=? WHERE " + col.NAME + "=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
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
    public DataSourceResult<String> getEmail(String user) {
        String sql = "SELECT " + col.EMAIL + " FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return DataSourceResult.of(rs.getString(1));
                }
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return DataSourceResult.unknownPlayer();
    }

    @Override
    public List<PlayerAuth> getAllAuths() {
        List<PlayerAuth> auths = new ArrayList<>();
        String sql = "SELECT * FROM " + tableName + ";";
        try (PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                PlayerAuth auth = buildAuthFromResultSet(rs);
                auths.add(auth);
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return auths;
    }

    @Override
    public List<String> getLoggedPlayersWithEmptyMail() {
        List<String> players = new ArrayList<>();
        String sql = "SELECT " + col.REAL_NAME + " FROM " + tableName + " WHERE " + col.IS_LOGGED + " = 1"
            + " AND (" + col.EMAIL + " = 'your@email.com' OR " + col.EMAIL + " IS NULL);";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                players.add(rs.getString(1));
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return players;
    }

    @Override
    public List<PlayerAuth> getRecentlyLoggedInPlayers() {
        List<PlayerAuth> players = new ArrayList<>();
        String sql = "SELECT * FROM " + tableName + " ORDER BY " + col.LAST_LOGIN + " DESC LIMIT 10;";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                players.add(buildAuthFromResultSet(rs));
            }
        } catch (SQLException e) {
            logSqlException(e);
        }
        return players;
    }

    private PlayerAuth buildAuthFromResultSet(ResultSet row) throws SQLException {
        String salt = !col.SALT.isEmpty() ? row.getString(col.SALT) : null;

        return PlayerAuth.builder()
            .name(row.getString(col.NAME))
            .email(row.getString(col.EMAIL))
            .realName(row.getString(col.REAL_NAME))
            .password(row.getString(col.PASSWORD), salt)
            .lastLogin(getNullableLong(row, col.LAST_LOGIN))
            .lastIp(row.getString(col.LAST_IP))
            .registrationDate(row.getLong(col.REGISTRATION_DATE))
            .registrationIp(row.getString(col.REGISTRATION_IP))
            .locX(row.getDouble(col.LASTLOC_X))
            .locY(row.getDouble(col.LASTLOC_Y))
            .locZ(row.getDouble(col.LASTLOC_Z))
            .locWorld(row.getString(col.LASTLOC_WORLD))
            .locYaw(row.getFloat(col.LASTLOC_YAW))
            .locPitch(row.getFloat(col.LASTLOC_PITCH))
            .build();
    }

    /**
     * Creates the column for registration date and sets all entries to the current timestamp.
     * We do so in order to avoid issues with purging, where entries with 0 / NULL might get
     * purged immediately on startup otherwise.
     *
     * @param st Statement object to the database
     */
    private void addRegistrationDateColumn(Statement st) throws SQLException {
        st.executeUpdate("ALTER TABLE " + tableName
            + " ADD COLUMN " + col.REGISTRATION_DATE + " TIMESTAMP NOT NULL DEFAULT '0';");

        // Use the timestamp from Java to avoid timezone issues in case JVM and database are out of sync
        long currentTimestamp = System.currentTimeMillis();
        int updatedRows = st.executeUpdate(String.format("UPDATE %s SET %s = %d;",
            tableName, col.REGISTRATION_DATE, currentTimestamp));
        ConsoleLogger.info("Created column '" + col.REGISTRATION_DATE + "' and set the current timestamp, "
            + currentTimestamp + ", to all " + updatedRows + " rows");
    }

    private static void close(Statement st) {
        if (st != null) {
            try {
                st.close();
            } catch (SQLException ex) {
                logSqlException(ex);
            }
        }
    }

    private static void close(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException ex) {
                logSqlException(ex);
            }
        }
    }
}
