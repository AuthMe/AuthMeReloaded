package fr.xephi.authme.datasource.converter;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.util.UuidUtils;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.xephi.authme.util.Utils.logAndSendMessage;

/**
 * Converts data from LibreLogin to AuthMe.
 * <p>
 * The storage backend is determined by reading {@code plugins/LibreLogin/config.conf}
 * ({@code database.type}):
 * <ul>
 *   <li><b>librelogin-sqlite</b> (default) — reads the SQLite database at the path configured
 *       under {@code sqlite.path} (default: {@code plugins/LibreLogin/user-data.db}).</li>
 *   <li><b>librelogin-mysql / librelogin-postgresql</b> — uses AuthMe's existing connection
 *       pool; LibreLogin must share the same database as AuthMe.</li>
 * </ul>
 * <p>
 * <b>Algorithm mapping:</b>
 * <ul>
 *   <li>{@code BCrypt-2A} → configure AuthMe with {@code passwordHash: BCRYPT}</li>
 *   <li>{@code Argon2-ID} → configure AuthMe with {@code passwordHash: ARGON2}</li>
 *   <li>{@code SHA-256} → configure AuthMe with {@code passwordHash: SHA256}</li>
 *   <li>{@code SHA-512} → configure AuthMe with {@code passwordHash: DOUBLE_SHA512}</li>
 *   <li>{@code LOGIT-SHA-256} → configure AuthMe with {@code passwordHash: SALTEDSHA256}</li>
 * </ul>
 */
public class LibreLoginConverter implements Converter {

    private static final String CONFIG_PATH = "plugins/LibreLogin/config.conf";
    private static final String DEFAULT_SQLITE_FILENAME = "user-data.db";
    private static final String TABLE = "librepremium_data";
    private static final String QUERY = "SELECT last_nickname, hashed_password, salt, algo, "
        + "ip, email, joined, last_seen, uuid, premium_uuid, secret FROM " + TABLE;

    private static final ConsoleLogger logger = ConsoleLoggerFactory.get(LibreLoginConverter.class);
    private final DataSource dataSource;

    @Inject
    LibreLoginConverter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void execute(CommandSender sender) {
        File configFile = new File(CONFIG_PATH);
        if (!configFile.exists()) {
            logAndSendMessage(sender, "LibreLogin conversion failed: config not found at " + CONFIG_PATH);
            return;
        }

        String dbType = readHoconValue(configFile, "type", "librelogin-sqlite");

        switch (dbType) {
            case "librelogin-sqlite": {
                String sqliteFilename = readHoconValue(configFile, "path", DEFAULT_SQLITE_FILENAME);
                new SqliteImport(dataSource, new File("plugins/LibreLogin/" + sqliteFilename)).execute(sender);
                break;
            }
            case "librelogin-mysql":
            case "librelogin-postgresql":
                new SqlImport(dataSource).execute(sender);
                break;
            default:
                logAndSendMessage(sender,
                    "LibreLogin conversion: database type '" + dbType + "' is not supported.");
        }
    }

    private static void importRows(ResultSet rs, DataSource dataSource, CommandSender sender) throws SQLException {
        long imported = 0;
        long skipped = 0;
        while (rs.next()) {
            String realName = rs.getString("last_nickname");
            if (realName == null || realName.isEmpty()) {
                continue;
            }
            String name = realName.toLowerCase(Locale.ROOT);

            if (dataSource.isAuthAvailable(name)) {
                ++skipped;
                continue;
            }

            HashedPassword password = buildHashedPassword(
                rs.getString("hashed_password"),
                rs.getString("salt"),
                rs.getString("algo"),
                name);
            if (password == null) {
                continue;
            }

            Timestamp joined = rs.getTimestamp("joined");
            Timestamp lastSeen = rs.getTimestamp("last_seen");

            PlayerAuth auth = PlayerAuth.builder()
                .name(name)
                .realName(realName)
                .password(password)
                .lastIp(rs.getString("ip"))
                .email(rs.getString("email"))
                .registrationDate(joined != null ? joined.getTime() : 0L)
                .lastLogin(lastSeen != null ? lastSeen.getTime() : null)
                .totpKey(rs.getString("secret"))
                .uuid(UuidUtils.parseUuidSafely(rs.getString("uuid")))
                .premiumUuid(UuidUtils.parseUuidSafely(rs.getString("premium_uuid")))
                .build();

            dataSource.saveAuth(auth);
            ++imported;
        }

        logAndSendMessage(sender, "LibreLogin conversion: " + imported + " account(s) imported, "
            + skipped + " skipped (already exist)");
    }

    private static HashedPassword buildHashedPassword(String hash, String salt, String algo, String name) {
        if (hash == null || algo == null) {
            return null;
        }
        switch (algo) {
            case "BCrypt-2A":
                return new HashedPassword(hash);

            case "Argon2-ID": {
                String phc = decodeArgon2(hash);
                if (phc == null) {
                    logger.warning("Could not decode Argon2-ID hash for player '" + name + "', skipping");
                    return null;
                }
                return new HashedPassword(phc);
            }

            case "SHA-256":
                // LibreLogin SHA-256: sha256(sha256(pw) + salt) — same as AuthMe SHA256: $SHA$<salt>$<hash>
                if (salt == null) {
                    logger.warning("Null salt for SHA-256 account '" + name + "', skipping");
                    return null;
                }
                return new HashedPassword("$SHA$" + salt + "$" + hash);

            case "SHA-512":
                // LibreLogin SHA-512: sha512(sha512(pw) + salt) — maps to AuthMe DOUBLE_SHA512
                if (salt == null) {
                    logger.warning("Null salt for SHA-512 account '" + name + "', skipping");
                    return null;
                }
                return new HashedPassword(hash, salt);

            case "LOGIT-SHA-256":
                // LibreLogin LOGIT-SHA-256: sha256(pw + salt) — maps to AuthMe SALTEDSHA256
                if (salt == null) {
                    logger.warning("Null salt for LOGIT-SHA-256 account '" + name + "', skipping");
                    return null;
                }
                return new HashedPassword(hash, salt);

            default:
                logger.warning("Unknown LibreLogin algorithm '" + algo + "' for player '" + name + "', skipping");
                return null;
        }
    }

    private static String decodeArgon2(String value) {
        if (value.startsWith("$argon2")) {
            return value;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            if (decoded.startsWith("$argon2")) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // fall through
        }
        return null;
    }

    /**
     * Reads a scalar value from a HOCON config file by scanning for the first line matching
     * {@code <key> = <value>} or {@code <key>: <value>} (with optional surrounding whitespace
     * and quotes). Sufficient for LibreLogin's flat scalar keys; not a full HOCON parser.
     */
    private static String readHoconValue(File file, String key, String defaultValue) {
        Pattern pattern = Pattern.compile(
            "^\\s*" + Pattern.quote(key) + "\\s*[=:]\\s*\"?([^\",\\s}#]+)\"?");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = pattern.matcher(line);
                if (m.find()) {
                    return m.group(1);
                }
            }
        } catch (IOException e) {
            // fall through to default
        }
        return defaultValue;
    }

    private static final class SqliteImport extends AbstractSqlitePluginConverter {

        private final File dbFile;

        SqliteImport(DataSource dataSource, File dbFile) {
            super(dataSource);
            this.dbFile = dbFile;
        }

        @Override
        public void execute(CommandSender sender) {
            if (!dbFile.exists()) {
                logAndSendMessage(sender,
                    "LibreLogin conversion failed: SQLite database not found at " + dbFile.getPath());
                return;
            }

            try (Connection conn = openConnection(dbFile);
                 PreparedStatement ps = conn.prepareStatement(QUERY);
                 ResultSet rs = ps.executeQuery()) {
                importRows(rs, getDataSource(), sender);
            } catch (SQLException e) {
                logAndSendMessage(sender, "LibreLogin conversion failed: " + e.getMessage());
                logger.logException("LibreLogin conversion error:", e);
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
                logAndSendMessage(sender, "LibreLogin conversion failed: " + e.getMessage());
                logger.logException("LibreLogin conversion error:", e);
            }
        }
    }
}
