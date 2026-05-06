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
 * Converts data from LimboAuth to AuthMe.
 * <p>
 * LimboAuth stores accounts in the {@code AUTH} table of the configured database.
 * This converter expects that LimboAuth and AuthMe share the same MySQL/MariaDB database.
 * <p>
 * LimboAuth uses BCrypt exclusively for new registrations, so configure AuthMe with
 * {@code passwordHash: BCRYPT} before running this converter.
 */
public class LimboAuthConverter extends AbstractSqlPluginConverter {

    private static final String TABLE = "AUTH";
    private static final String QUERY = "SELECT NICKNAME, LOWERCASENICKNAME, HASH, IP, "
        + "REGDATE, LOGINDATE, UUID, PREMIUMUUID, TOTPTOKEN FROM " + TABLE;

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(LimboAuthConverter.class);

    @Inject
    LimboAuthConverter(DataSource dataSource) {
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
                String realName = rs.getString("NICKNAME");
                String name = rs.getString("LOWERCASENICKNAME");
                if (name == null || name.isEmpty()) {
                    if (realName != null) {
                        name = realName.toLowerCase(Locale.ROOT);
                    } else {
                        continue;
                    }
                }
                if (realName == null) {
                    realName = name;
                }

                if (getDataSource().isAuthAvailable(name)) {
                    ++skipped;
                    continue;
                }

                String hash = rs.getString("HASH");
                if (hash == null || hash.isEmpty()) {
                    logger.warning("No hash for player '" + name + "', skipping");
                    continue;
                }

                long regDate = rs.getLong("REGDATE");
                long loginDate = rs.getLong("LOGINDATE");

                PlayerAuth.Builder builder = PlayerAuth.builder()
                    .name(name)
                    .realName(realName)
                    .password(new HashedPassword(hash))
                    .lastIp(rs.getString("IP"))
                    .registrationDate(regDate)
                    .lastLogin(loginDate > 0 ? loginDate : null)
                    .totpKey(rs.getString("TOTPTOKEN"))
                    .uuid(UuidUtils.parseUuidSafely(rs.getString("UUID")))
                    .premiumUuid(UuidUtils.parseUuidSafely(rs.getString("PREMIUMUUID")));

                getDataSource().saveAuth(builder.build());
                ++imported;
            }

            logAndSendMessage(sender, "LimboAuth conversion: " + imported + " account(s) imported, "
                + skipped + " skipped (already exist)");

        } catch (SQLException e) {
            logAndSendMessage(sender, "LimboAuth conversion failed: " + e.getMessage());
            logger.logException("LimboAuth conversion error:", e);
        }
    }
}
