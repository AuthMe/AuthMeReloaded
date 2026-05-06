package fr.xephi.authme.datasource.converter;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.security.crypts.HashedPassword;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import static fr.xephi.authme.util.Utils.logAndSendMessage;

/**
 * Converts data from OpeNLogin to AuthMe.
 * <p>
 * OpeNLogin stores accounts in a SQLite file at {@code plugins/OpeNLogin/accounts.db}.
 * No shared database is required; the converter reads the file directly.
 * <p>
 * OpeNLogin uses BCrypt exclusively. Set {@code passwordHash} to {@code BCRYPT} in AuthMe's
 * {@code config.yml} before running the conversion.
 */
public class OpeNLoginConverter extends AbstractSqlitePluginConverter {

    private static final String DB_PATH = "plugins/OpeNLogin/accounts.db";
    private static final String QUERY =
        "SELECT name, realname, password, address, lastlogin, regdate FROM openlogin";

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(OpeNLoginConverter.class);

    @Inject
    OpeNLoginConverter(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void execute(CommandSender sender) {
        File dbFile = new File(DB_PATH);
        if (!dbFile.exists()) {
            logAndSendMessage(sender, "OpeNLogin conversion failed: database file not found at " + DB_PATH);
            return;
        }

        try (Connection conn = openConnection(dbFile);
             PreparedStatement ps = conn.prepareStatement(QUERY);
             ResultSet rs = ps.executeQuery()) {

            long imported = 0;
            long skipped = 0;
            while (rs.next()) {
                String name = rs.getString("name");
                if (name == null || name.isEmpty()) {
                    continue;
                }
                name = name.toLowerCase(Locale.ROOT);

                if (getDataSource().isAuthAvailable(name)) {
                    ++skipped;
                    continue;
                }

                String hash = rs.getString("password");
                if (hash == null || hash.isEmpty()) {
                    logger.warning("No password for player '" + name + "', skipping");
                    continue;
                }

                String realName = rs.getString("realname");
                long lastLogin = rs.getLong("lastlogin");

                PlayerAuth auth = PlayerAuth.builder()
                    .name(name)
                    .realName(realName != null && !realName.isEmpty() ? realName : name)
                    .password(new HashedPassword(hash))
                    .lastIp(rs.getString("address"))
                    .registrationDate(rs.getLong("regdate"))
                    .lastLogin(lastLogin > 0 ? lastLogin : null)
                    .build();

                getDataSource().saveAuth(auth);
                ++imported;
            }

            logAndSendMessage(sender, "OpeNLogin conversion: " + imported + " account(s) imported, "
                + skipped + " skipped (already exist)");

        } catch (SQLException e) {
            logAndSendMessage(sender, "OpeNLogin conversion failed: " + e.getMessage());
            logger.logException("OpeNLogin conversion error:", e);
        }
    }
}
