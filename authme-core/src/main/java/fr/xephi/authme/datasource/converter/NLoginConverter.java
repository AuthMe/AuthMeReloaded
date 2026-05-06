package fr.xephi.authme.datasource.converter;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.util.UuidUtils;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import static fr.xephi.authme.util.Utils.logAndSendMessage;

/**
 * Converts data from nLogin to AuthMe.
 * <p>
 * nLogin stores accounts in the {@code nlogin} table of the configured database.
 * This converter expects that nLogin and AuthMe share the same MySQL/MariaDB database.
 * <p>
 * nLogin deliberately reuses AuthMe's password hash formats for BCrypt, SHA-256 and SHA-512, so
 * hashes are copied as-is. Configure AuthMe's {@code passwordHash} to match the algorithm used by
 * nLogin (default: {@code BCRYPT}). Argon2 hashes are also supported via {@code ARGON2}.
 */
public class NLoginConverter extends AbstractSqlPluginConverter {

    private static final String TABLE = "nlogin";
    private static final String QUERY = "SELECT last_name, password, last_ip, email, "
        + "creation_date, last_login, unique_id FROM " + TABLE;

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(NLoginConverter.class);

    @Inject
    NLoginConverter(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void execute(CommandSender sender) {
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(QUERY);
             ResultSet rs = ps.executeQuery()) {

            long imported = 0;
            long skipped = 0;
            while (rs.next()) {
                String realName = rs.getString("last_name");
                if (realName == null || realName.isEmpty()) {
                    continue;
                }
                String name = realName.toLowerCase(Locale.ROOT);

                if (getDataSource().isAuthAvailable(name)) {
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

                getDataSource().saveAuth(builder.build());
                ++imported;
            }

            logAndSendMessage(sender, "nLogin conversion: " + imported + " account(s) imported, "
                + skipped + " skipped (already exist)");

        } catch (SQLException e) {
            logAndSendMessage(sender, "nLogin conversion failed: " + e.getMessage());
            logger.logException("nLogin conversion error:", e);
        }
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
