package fr.xephi.authme.datasource.converter;

import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ConverterSettings;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static fr.xephi.authme.util.Utils.logAndSendMessage;

/**
 * Converts data from LoginSecurity to AuthMe.
 */
public class LoginSecurityConverter implements Converter {

    private final File dataFolder;
    private final DataSource dataSource;

    private final boolean useSqlite;
    private final String mySqlHost;
    private final String mySqlDatabase;
    private final String mySqlUser;
    private final String mySqlPassword;

    @Inject
    LoginSecurityConverter(@DataFolder File dataFolder, DataSource dataSource, Settings settings) {
        this.dataFolder = dataFolder;
        this.dataSource = dataSource;

        useSqlite = settings.getProperty(ConverterSettings.LOGINSECURITY_USE_SQLITE);
        mySqlHost = settings.getProperty(ConverterSettings.LOGINSECURITY_MYSQL_HOST);
        mySqlDatabase = settings.getProperty(ConverterSettings.LOGINSECURITY_MYSQL_DATABASE);
        mySqlUser = settings.getProperty(ConverterSettings.LOGINSECURITY_MYSQL_USER);
        mySqlPassword = settings.getProperty(ConverterSettings.LOGINSECURITY_MYSQL_PASSWORD);
    }

    @Override
    public void execute(CommandSender sender) {
        try (Connection connection = createConnectionOrInformSender(sender)) {
            if (connection != null) {
                performConversion(sender, connection);
            }
        } catch (SQLException e) {
            sender.sendMessage("Failed to convert from SQLite. Please see the log for more info");
            ConsoleLogger.logException("Could not fetch or migrate data:", e);
        }
    }

    /**
     * Performs the conversion from LoginSecurity to AuthMe.
     *
     * @param sender the command sender who launched the conversion
     * @param connection connection to the LoginSecurity data source
     */
    @VisibleForTesting
    void performConversion(CommandSender sender, Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                "SELECT * from ls_players LEFT JOIN ls_locations ON ls_locations.id = ls_players.id");
            try (ResultSet resultSet = statement.getResultSet()) {
                migrateData(sender, resultSet);
            }
        }
    }

    /**
     * Migrates the accounts.
     *
     * @param sender the command sender
     * @param resultSet result set with the account data to migrate
     */
    private void migrateData(CommandSender sender, ResultSet resultSet) throws SQLException {
        List<String> skippedPlayers = new ArrayList<>();
        long successfulSaves = 0;
        while (resultSet.next()) {
            String name = resultSet.getString("last_name");
            if (dataSource.isAuthAvailable(name)) {
                skippedPlayers.add(name);
            } else {
                PlayerAuth auth = buildAuthFromLoginSecurity(name, resultSet);
                dataSource.saveAuth(auth);
                dataSource.updateSession(auth);
                ++successfulSaves;
            }
        }

        logAndSendMessage(sender, "Migrated " + successfulSaves + " accounts successfully from LoginSecurity");
        if (!skippedPlayers.isEmpty()) {
            logAndSendMessage(sender, "Skipped conversion for players which were already in AuthMe: "
                + String.join(", ", skippedPlayers));
        }
    }

    /**
     * Creates a PlayerAuth based on data extracted from the given result set.
     *
     * @param name the name of the player to build
     * @param resultSet the result set to extract data from
     * @return the created player auth object
     */
    private static PlayerAuth buildAuthFromLoginSecurity(String name, ResultSet resultSet) throws SQLException {
        Long lastLoginMillis = Optional.ofNullable(resultSet.getTimestamp("last_login"))
            .map(Timestamp::getTime).orElse(null);
        long regDate = Optional.ofNullable(resultSet.getDate("registration_date"))
            .map(Date::getTime).orElse(System.currentTimeMillis());
        return PlayerAuth.builder()
            .name(name)
            .realName(name)
            .password(resultSet.getString("password"), null)
            .lastIp(resultSet.getString("ip_address"))
            .lastLogin(lastLoginMillis)
            .registrationDate(regDate)
            .locX(resultSet.getDouble("x"))
            .locY(resultSet.getDouble("y"))
            .locZ(resultSet.getDouble("z"))
            .locWorld(resultSet.getString("world"))
            .locYaw(resultSet.getFloat("yaw"))
            .locPitch(resultSet.getFloat("pitch"))
            .build();
    }

    /**
     * Creates a {@link Connection} to the LoginSecurity data source based on the settings,
     * or informs the sender of the error that occurred.
     *
     * @param sender the command sender who launched the conversion
     * @return the created connection object, or null if it failed
     */
    private Connection createConnectionOrInformSender(CommandSender sender) {
        Connection connection;
        if (useSqlite) {
            File sqliteDatabase = new File(dataFolder.getParentFile(), "LoginSecurity/LoginSecurity.db");
            if (!sqliteDatabase.exists()) {
                sender.sendMessage("The file '" + sqliteDatabase.getPath() + "' does not exist");
                return null;
            }
            connection = createSqliteConnection("plugins/LoginSecurity/LoginSecurity.db");
        } else {
            if (mySqlDatabase.isEmpty() || mySqlUser.isEmpty()) {
                sender.sendMessage("The LoginSecurity database or username is not configured in AuthMe's config.yml");
                return null;
            }
            connection = createMySqlConnection();
        }

        if (connection == null) {
            sender.sendMessage("Could not connect to LoginSecurity using Sqlite = "
                + useSqlite + ", see log for more info");
            return null;
        }
        return connection;
    }

    /**
     * Creates a connection to SQLite.
     *
     * @param path the path to the SQLite database
     * @return the created connection
     */
    @VisibleForTesting
    Connection createSqliteConnection(String path) {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }

        try {
            return DriverManager.getConnection(
                "jdbc:sqlite:" + path, "trump", "donald");
        } catch (SQLException e) {
            ConsoleLogger.logException("Could not connect to SQLite database", e);
            return null;
        }
    }

    private Connection createMySqlConnection() {
        try {
            return DriverManager.getConnection(
                "jdbc:mysql://" + mySqlHost + "/" + mySqlDatabase, mySqlUser, mySqlPassword);
        } catch (SQLException e) {
            ConsoleLogger.logException("Could not connect to SQLite database", e);
            return null;
        }
    }
}
