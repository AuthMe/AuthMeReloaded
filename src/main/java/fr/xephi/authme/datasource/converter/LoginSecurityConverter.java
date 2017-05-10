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
import java.util.ArrayList;
import java.util.List;

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
                ++successfulSaves;
            }
        }

        logAndSendMessage(sender, "Migrated " + successfulSaves + " accounts successfully from LoginSecurity");
        if (!skippedPlayers.isEmpty()) {
            logAndSendMessage(sender, "Skipped conversion for players which were already in AuthMe: "
                + String.join(", ", skippedPlayers));
        }
    }

    private static PlayerAuth buildAuthFromLoginSecurity(String name, ResultSet resultSet) throws SQLException {
        return PlayerAuth.builder()
            .name(name)
            .realName(name)
            .password(resultSet.getString("password"), null)
            .ip(resultSet.getString("ip_address"))
            .lastLogin(resultSet.getLong("last_login"))
            // TODO #792: Register date
            .locX(resultSet.getDouble("x"))
            .locY(resultSet.getDouble("y"))
            .locZ(resultSet.getDouble("z"))
            .locWorld(resultSet.getString("world"))
            .locYaw(resultSet.getFloat("yaw"))
            .locPitch(resultSet.getFloat("pitch"))
            .build();
    }

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
