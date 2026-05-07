package fr.xephi.authme.datasource.converter;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.util.UuidUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.inject.Inject;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import static fr.xephi.authme.util.Utils.logAndSendMessage;

/**
 * Converts data from nLogin to AuthMe.
 * <p>
 * The storage backend is determined by reading {@code plugins/nLogin/config.yml}:
 * <ul>
 *   <li><b>SQLite</b> ({@code database.type: SQLite}) — opens {@code plugins/nLogin/nlogin.db}
 *       directly.</li>
 *   <li><b>MySQL / MariaDB</b> — opens a dedicated JDBC connection using nLogin's own credentials
 *       ({@code database.remote.*}); AuthMe may use any datasource type.</li>
 * </ul>
 * The account table name is read from {@code database.table.account.table-name} (default:
 * {@code nlogin}).
 * <p>
 * nLogin deliberately reuses AuthMe's password hash formats for BCrypt, SHA-256 and SHA-512, so
 * hashes are copied as-is. Configure AuthMe's {@code passwordHash} to match the algorithm used by
 * nLogin (default: {@code BCRYPT}). Argon2 hashes are also supported via {@code ARGON2}.
 */
public class NLoginConverter implements Converter {

    private static final String CONFIG_PATH = "plugins/nLogin/config.yml";
    private static final String SQLITE_DB_PATH = "plugins/nLogin/nlogin.db";
    private static final String DEFAULT_TABLE = "nlogin";

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(NLoginConverter.class);
    private final DataSource dataSource;

    @Inject
    NLoginConverter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void execute(CommandSender sender) {
        File configFile = new File(CONFIG_PATH);
        if (!configFile.exists()) {
            logAndSendMessage(sender, "nLogin conversion failed: config not found at " + CONFIG_PATH);
            return;
        }

        YamlConfiguration nLoginConfig = YamlConfiguration.loadConfiguration(configFile);
        String dbType = nLoginConfig.getString("database.type", "SQLite");
        String table = nLoginConfig.getString("database.table.account.table-name", DEFAULT_TABLE);
        String query = "SELECT last_name, password, last_ip, email, creation_date, last_login, unique_id FROM " + table;

        try (Connection conn = openConnection(dbType, nLoginConfig);
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            long imported = 0;
            long skipped = 0;
            while (rs.next()) {
                String realName = rs.getString("last_name");
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

                long creationDate = rs.getLong("creation_date");
                long lastLogin = rs.getLong("last_login");

                PlayerAuth.Builder builder = PlayerAuth.builder()
                    .name(name)
                    .realName(realName)
                    .password(new HashedPassword(hash))
                    .lastIp(rs.getString("last_ip"))
                    .email(rs.getString("email"))
                    .registrationDate(creationDate)
                    .lastLogin(lastLogin > 0 ? lastLogin : null)
                    .uuid(parseNLoginUuid(rs.getString("unique_id")));

                dataSource.saveAuth(builder.build());
                ++imported;
            }

            logAndSendMessage(sender, "nLogin conversion: " + imported + " account(s) imported, "
                + skipped + " skipped (already exist)");

        } catch (SQLException e) {
            logAndSendMessage(sender, "nLogin conversion failed: " + e.getMessage());
            logger.logException("nLogin conversion error:", e);
        }
    }

    private Connection openConnection(String dbType, YamlConfiguration nLoginConfig) throws SQLException {
        if ("SQLite".equalsIgnoreCase(dbType)) {
            File sqliteFile = new File(SQLITE_DB_PATH);
            if (!sqliteFile.exists()) {
                throw new SQLException("nLogin SQLite database not found at '" + SQLITE_DB_PATH + "'");
            }
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQLite JDBC driver not available", e);
            }
            return DriverManager.getConnection("jdbc:sqlite:" + sqliteFile.getAbsolutePath());
        }

        // MySQL or MariaDB: open a dedicated connection using nLogin's own credentials
        String hostname = nLoginConfig.getString("database.remote.hostname", "localhost:3306");
        String database = nLoginConfig.getString("database.remote.database", "nLogin");
        String username = nLoginConfig.getString("database.remote.username", "root");
        String password = nLoginConfig.getString("database.remote.password", "");

        String[] hostParts = hostname.split(":", 2);
        String host = hostParts[0];
        String port = hostParts.length > 1 ? hostParts[1] : "3306";

        String scheme = "MariaDB".equalsIgnoreCase(dbType) ? "mariadb" : "mysql";
        String url = "jdbc:" + scheme + "://" + host + ":" + port + "/" + database
            + "?useUnicode=true&characterEncoding=utf8";
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Parses a UUID from a 32-char hex string (no dashes, nLogin's format) or a standard UUID string.
     */
    private static java.util.UUID parseNLoginUuid(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        // nLogin stores UUIDs without dashes (32 hex chars)
        if (value.length() == 32) {
            value = value.substring(0, 8) + "-"
                + value.substring(8, 12) + "-"
                + value.substring(12, 16) + "-"
                + value.substring(16, 20) + "-"
                + value.substring(20);
        }
        return UuidUtils.parseUuidSafely(value);
    }
}
