package fr.xephi.authme.datasource.converter;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.security.crypts.HashedPassword;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.inject.Inject;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import static fr.xephi.authme.util.Utils.logAndSendMessage;

/**
 * Converts data from tiAuth to AuthMe.
 * <p>
 * The storage backend is determined by reading {@code plugins/tiAuth/config.yml}:
 * <ul>
 *   <li><b>SQLite</b> ({@code database.type: SQLITE}) — reads {@code plugins/tiAuth/auth.db}
 *       directly.</li>
 *   <li><b>MySQL / PostgreSQL</b> — uses AuthMe's existing connection pool; tiAuth must share
 *       the same database as AuthMe.</li>
 *   <li><b>H2</b> (default) — not supported; reconfigure tiAuth to use SQLite or MySQL and
 *       migrate the data before running this converter.</li>
 * </ul>
 * <p>
 * tiAuth BCrypt hashes ({@code $2a$...}) are compatible with AuthMe's {@code BCRYPT} algorithm.
 * tiAuth SHA-256 hashes are stored as {@code $SHA$<salt>$<hash>}, which is identical to AuthMe's
 * {@code SHA256} format. Configure {@code passwordHash} in AuthMe's {@code config.yml} to match
 * the algorithm used in tiAuth before running the conversion.
 */
public class TiAuthConverter implements Converter {

    private static final String CONFIG_PATH = "plugins/tiAuth/config.yml";
    private static final String SQLITE_DB_PATH = "plugins/tiAuth/auth.db";
    private static final String QUERY =
        "SELECT username, realName, password, lastIp, lastLogin, regDate FROM auth_users";

    private static final ConsoleLogger logger = ConsoleLoggerFactory.get(TiAuthConverter.class);
    private final DataSource dataSource;

    @Inject
    TiAuthConverter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void execute(CommandSender sender) {
        File configFile = new File(CONFIG_PATH);
        if (!configFile.exists()) {
            logAndSendMessage(sender, "tiAuth conversion failed: config not found at " + CONFIG_PATH);
            return;
        }

        YamlConfiguration tiAuthConfig = YamlConfiguration.loadConfiguration(configFile);
        String dbType = tiAuthConfig.getString("database.type", "H2").toUpperCase(Locale.ROOT);

        switch (dbType) {
            case "SQLITE":
                new SqliteImport(dataSource).execute(sender);
                break;
            case "MYSQL":
            case "POSTGRESQL":
                new SqlImport(dataSource).execute(sender);
                break;
            default:
                logAndSendMessage(sender,
                    "tiAuth conversion: database type '" + dbType + "' is not supported. "
                    + "Reconfigure tiAuth to use SQLite or MySQL/PostgreSQL, migrate the data, "
                    + "then re-run the conversion.");
        }
    }

    private static void importRows(ResultSet rs, DataSource dataSource, CommandSender sender) throws SQLException {
        long imported = 0;
        long skipped = 0;
        while (rs.next()) {
            String realName = rs.getString("realName");
            if (realName == null || realName.isEmpty()) {
                continue;
            }
            String name = realName.toLowerCase(Locale.ROOT);

            if (dataSource.isAuthAvailable(name)) {
                ++skipped;
                continue;
            }

            String hash = rs.getString("password");
            if (hash == null || hash.isEmpty()) {
                logger.warning("No password for player '" + name + "', skipping");
                continue;
            }

            long lastLogin = rs.getLong("lastLogin");
            long regDate = rs.getLong("regDate");

            PlayerAuth auth = PlayerAuth.builder()
                .name(name)
                .realName(realName)
                .password(new HashedPassword(hash))
                .lastIp(rs.getString("lastIp"))
                .registrationDate(regDate)
                .lastLogin(lastLogin > 0 ? lastLogin : null)
                .build();

            dataSource.saveAuth(auth);
            ++imported;
        }

        logAndSendMessage(sender, "tiAuth conversion: " + imported + " account(s) imported, "
            + skipped + " skipped (already exist)");
    }

    private static final class SqliteImport extends AbstractSqlitePluginConverter {

        SqliteImport(DataSource dataSource) {
            super(dataSource);
        }

        @Override
        public void execute(CommandSender sender) {
            File dbFile = new File(SQLITE_DB_PATH);
            if (!dbFile.exists()) {
                logAndSendMessage(sender, "tiAuth conversion failed: SQLite database not found at " + SQLITE_DB_PATH);
                return;
            }

            try (Connection conn = openConnection(dbFile);
                 PreparedStatement ps = conn.prepareStatement(QUERY);
                 ResultSet rs = ps.executeQuery()) {
                importRows(rs, getDataSource(), sender);
            } catch (SQLException e) {
                logAndSendMessage(sender, "tiAuth conversion failed: " + e.getMessage());
                logger.logException("tiAuth conversion error:", e);
            }
        }
    }

    private static final class SqlImport extends AbstractSqlPluginConverter {

        SqlImport(DataSource dataSource) {
            super(dataSource);
        }

        @Override
        public void execute(CommandSender sender) {
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement(QUERY);
                 ResultSet rs = ps.executeQuery()) {
                importRows(rs, getDataSource(), sender);
            } catch (SQLException e) {
                logAndSendMessage(sender, "tiAuth conversion failed: " + e.getMessage());
                logger.logException("tiAuth conversion error:", e);
            }
        }
    }
}
