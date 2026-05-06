package fr.xephi.authme.datasource.converter;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.UuidUtils;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Locale;
import java.nio.charset.StandardCharsets;

import static fr.xephi.authme.util.Utils.logAndSendMessage;

/**
 * Converts data from LibreLogin to AuthMe.
 * <p>
 * LibreLogin stores accounts in the {@code librepremium_data} table of the configured database.
 * This converter expects that LibreLogin and AuthMe share the same MySQL/MariaDB database.
 * <p>
 * <b>Algorithm mapping:</b>
 * <ul>
 *   <li>{@code BCrypt-2A} → configure AuthMe with {@code passwordHash: BCRYPT}</li>
 *   <li>{@code Argon2-ID} → configure AuthMe with {@code passwordHash: ARGON2}</li>
 *   <li>{@code SHA-256} → configure AuthMe with {@code passwordHash: SHA256} (same computation)</li>
 *   <li>{@code SHA-512} → configure AuthMe with {@code passwordHash: DOUBLE_SHA512}</li>
 *   <li>{@code LOGIT-SHA-256} → configure AuthMe with {@code passwordHash: SALTEDSHA256}</li>
 * </ul>
 * If accounts use mixed algorithms, set the most common one in AuthMe's config and have remaining
 * players reset their password.
 */
public class LibreLoginConverter extends AbstractSqlPluginConverter {

    private static final String TABLE = "librepremium_data";
    private static final String QUERY = "SELECT last_nickname, hashed_password, salt, algo, "
        + "ip, email, joined, last_seen, uuid, premium_uuid, secret FROM " + TABLE;

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(LibreLoginConverter.class);

    @Inject
    LibreLoginConverter(Settings settings, DataSource dataSource) {
        super(settings, dataSource);
    }

    @Override
    public void execute(CommandSender sender) {
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(QUERY);
             ResultSet rs = ps.executeQuery()) {

            long imported = 0;
            long skipped = 0;
            while (rs.next()) {
                String realName = rs.getString("last_nickname");
                if (realName == null || realName.isEmpty()) {
                    continue;
                }
                String name = realName.toLowerCase(Locale.ROOT);

                if (getDataSource().isAuthAvailable(name)) {
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

                PlayerAuth.Builder builder = PlayerAuth.builder()
                    .name(name)
                    .realName(realName)
                    .password(password)
                    .lastIp(rs.getString("ip"))
                    .email(rs.getString("email"))
                    .registrationDate(joined != null ? joined.getTime() : 0L)
                    .lastLogin(lastSeen != null ? lastSeen.getTime() : null)
                    .totpKey(rs.getString("secret"))
                    .uuid(UuidUtils.parseUuidSafely(rs.getString("uuid")))
                    .premiumUuid(UuidUtils.parseUuidSafely(rs.getString("premium_uuid")));

                getDataSource().saveAuth(builder.build());
                ++imported;
            }

            logAndSendMessage(sender, "LibreLogin conversion: " + imported + " account(s) imported, "
                + skipped + " skipped (already exist)");

        } catch (SQLException e) {
            logAndSendMessage(sender, "LibreLogin conversion failed: " + e.getMessage());
            logger.logException("LibreLogin conversion error:", e);
        }
    }

    private HashedPassword buildHashedPassword(String hash, String salt, String algo, String name) {
        if (hash == null || algo == null) {
            return null;
        }
        switch (algo) {
            case "BCrypt-2A":
                return new HashedPassword(hash);

            case "Argon2-ID":
                // LibreLogin stores Argon2 as a Base64-encoded PHC string
                String phc = decodeArgon2(hash);
                if (phc == null) {
                    logger.warning("Could not decode Argon2-ID hash for player '" + name + "', skipping");
                    return null;
                }
                return new HashedPassword(phc);

            case "SHA-256":
                // LibreLogin SHA-256: sha256(sha256(pw) + salt) — same computation as AuthMe SHA256.
                // Reconstruct the AuthMe format: $SHA$<salt>$<hash>
                if (salt == null) {
                    logger.warning("Null salt for SHA-256 account '" + name + "', skipping");
                    return null;
                }
                return new HashedPassword("$SHA$" + salt + "$" + hash);

            case "SHA-512":
                // LibreLogin SHA-512: sha512(sha512(pw) + salt) — maps to AuthMe DOUBLE_SHA512 (separate salt).
                if (salt == null) {
                    logger.warning("Null salt for SHA-512 account '" + name + "', skipping");
                    return null;
                }
                return new HashedPassword(hash, salt);

            case "LOGIT-SHA-256":
                // LibreLogin LOGIT-SHA-256: sha256(pw + salt) — maps to AuthMe SALTEDSHA256 (separate salt).
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

    private String decodeArgon2(String value) {
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
}
