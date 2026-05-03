package fr.xephi.authme.datasource;

import ch.jalu.datasourcecolumns.data.DataSourceValue;
import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.columnshandler.AuthMeColumnsHandler;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;

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
import java.util.Locale;
import java.util.Set;

import fr.xephi.authme.util.UuidUtils;

import java.util.UUID;

import static fr.xephi.authme.datasource.SqlDataSourceUtils.getNullableLong;
import static fr.xephi.authme.datasource.SqlDataSourceUtils.logSqlException;

/**
 * SQLite data source.
 */
@SuppressWarnings({"checkstyle:AbbreviationAsWordInName"}) // Justification: Class name cannot be changed anymore
public class SQLite extends AbstractSqlDataSource {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(SQLite.class);
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
            logger.logException("Error during SQLite initialization:", ex);
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
        this.columnsHandler = AuthMeColumnsHandler.createForSqlite(con, settings);
    }

    /**
     * Initializes the connection to the SQLite database.
     *
     * @throws SQLException when an SQL error occurs while connecting
     */
    protected void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Failed to load SQLite JDBC class", e);
        }

        logger.debug("SQLite driver loaded");
        this.con = DriverManager.getConnection(this.getJdbcUrl(this.dataFolder.getAbsolutePath(), "", this.database));
        this.columnsHandler = AuthMeColumnsHandler.createForSqlite(con, settings);
    }

    /**
     * Creates the table if necessary, or adds any missing columns to the table.
     *
     * @throws SQLException when an SQL error occurs while initializing the database
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:CyclomaticComplexity")
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

            if (isColumnMissing(md, col.TOTP_KEY)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.TOTP_KEY + " VARCHAR(32);");
            }

            if (!col.PLAYER_UUID.isEmpty() && isColumnMissing(md, col.PLAYER_UUID)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.PLAYER_UUID + " VARCHAR(36)");
            }

            if (!col.PREMIUM_UUID.isEmpty() && isColumnMissing(md, col.PREMIUM_UUID)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.PREMIUM_UUID + " VARCHAR(36)");
            }
        }
        logger.info("SQLite Setup finished");
    }

    /**
     * Migrates the database if necessary. See {@link SqLiteMigrater} for details.
     */
    @VisibleForTesting
    void migrateIfNeeded() throws SQLException {
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
    public synchronized void reload() {
        close(con);
        try {
            this.connect();
            this.setup();
            this.migrateIfNeeded();
        } catch (SQLException ex) {
            logger.logException("Error while reloading SQLite:", ex);
        }
    }

    @Override
    public synchronized PlayerAuth getAuth(String user) {
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
    public synchronized Set<String> getRecordsToPurge(long until) {
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
    public synchronized void purgeRecords(Collection<String> toPurge) {
        String delete = "DELETE FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (PreparedStatement deletePst = con.prepareStatement(delete)) {
            for (String name : toPurge) {
                deletePst.setString(1, name.toLowerCase(Locale.ROOT));
                deletePst.executeUpdate();
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
    }

    @Override
    public synchronized boolean removeAuth(String user) {
        String sql = "DELETE FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user.toLowerCase(Locale.ROOT));
            pst.executeUpdate();
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public synchronized void closeConnection() {
        try {
            if (con != null && !con.isClosed()) {
                con.close();
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.SQLITE;
    }

    @Override
    public synchronized List<PlayerAuth> getAllAuths() {
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
    public synchronized List<String> getLoggedPlayersWithEmptyMail() {
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
    public synchronized List<PlayerAuth> getRecentlyLoggedInPlayers() {
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


    @Override
    public synchronized boolean setTotpKey(String user, String totpKey) {
        String sql = "UPDATE " + tableName + " SET " + col.TOTP_KEY + " = ? WHERE " + col.NAME + " = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, totpKey);
            pst.setString(2, user.toLowerCase(Locale.ROOT));
            pst.executeUpdate();
            return true;
        } catch (SQLException e) {
            logSqlException(e);
        }
        return false;
    }

    private PlayerAuth buildAuthFromResultSet(ResultSet row) throws SQLException {
        String salt = !col.SALT.isEmpty() ? row.getString(col.SALT) : null;
        UUID premiumUuid = col.PREMIUM_UUID.isEmpty()
            ? null : UuidUtils.parseUuidSafely(row.getString(col.PREMIUM_UUID));

        return PlayerAuth.builder()
            .name(row.getString(col.NAME))
            .email(row.getString(col.EMAIL))
            .realName(row.getString(col.REAL_NAME))
            .password(row.getString(col.PASSWORD), salt)
            .totpKey(row.getString(col.TOTP_KEY))
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
            .premiumUuid(premiumUuid)
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
        logger.info("Created column '" + col.REGISTRATION_DATE + "' and set the current timestamp, "
            + currentTimestamp + ", to all " + updatedRows + " rows");
    }

    @Override
    String getJdbcUrl(String dataPath, String ignored, String database) {
        return "jdbc:sqlite:" + dataPath + File.separator + database + ".db";
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

    // ---------------------------------------------------------------------------
    // Synchronized wrappers for AbstractSqlDataSource methods
    // All operations share a single Connection, so we must serialize access.
    // ---------------------------------------------------------------------------

    @Override
    public synchronized boolean isAuthAvailable(String user) {
        return super.isAuthAvailable(user);
    }

    @Override
    public synchronized HashedPassword getPassword(String user) {
        return super.getPassword(user);
    }

    @Override
    public synchronized boolean saveAuth(PlayerAuth auth) {
        return super.saveAuth(auth);
    }

    @Override
    public synchronized boolean hasSession(String user) {
        return super.hasSession(user);
    }

    @Override
    public synchronized boolean updateSession(PlayerAuth auth) {
        return super.updateSession(auth);
    }

    @Override
    public synchronized boolean updatePassword(PlayerAuth auth) {
        return super.updatePassword(auth);
    }

    @Override
    public synchronized boolean updatePassword(String user, HashedPassword password) {
        return super.updatePassword(user, password);
    }

    @Override
    public synchronized boolean updateQuitLoc(PlayerAuth auth) {
        return super.updateQuitLoc(auth);
    }

    @Override
    public synchronized List<String> getAllAuthsByIp(String ip) {
        return super.getAllAuthsByIp(ip);
    }

    @Override
    public synchronized int countAuthsByEmail(String email) {
        return super.countAuthsByEmail(email);
    }

    @Override
    public synchronized boolean updateEmail(PlayerAuth auth) {
        return super.updateEmail(auth);
    }

    @Override
    public synchronized boolean isLogged(String user) {
        return super.isLogged(user);
    }

    @Override
    public synchronized void setLogged(String user) {
        super.setLogged(user);
    }

    @Override
    public synchronized void setUnlogged(String user) {
        super.setUnlogged(user);
    }

    @Override
    public synchronized void grantSession(String user) {
        super.grantSession(user);
    }

    @Override
    public synchronized void revokeSession(String user) {
        super.revokeSession(user);
    }

    @Override
    public synchronized void purgeLogged() {
        super.purgeLogged();
    }

    @Override
    public synchronized int getAccountsRegistered() {
        return super.getAccountsRegistered();
    }

    @Override
    public synchronized boolean updateRealName(String user, String realName) {
        return super.updateRealName(user, realName);
    }

    @Override
    public synchronized DataSourceValue<String> getEmail(String user) {
        return super.getEmail(user);
    }
}
