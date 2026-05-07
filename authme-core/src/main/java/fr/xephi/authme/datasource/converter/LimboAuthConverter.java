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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import static fr.xephi.authme.util.Utils.logAndSendMessage;

/**
 * Converts data from LimboAuth to AuthMe.
 * <p>
 * The storage backend is determined by reading {@code plugins/limboauth/config.yml}:
 * <ul>
 *   <li><b>SQLite</b> ({@code database.storage-type: SQLITE}) — reads
 *       {@code plugins/limboauth/limboauth.db} directly.</li>
 *   <li><b>MySQL / MariaDB / PostgreSQL</b> — uses AuthMe's existing connection pool; LimboAuth
 *       must share the same database as AuthMe.</li>
 *   <li><b>H2</b> (default) — not supported; reconfigure LimboAuth to use SQLite or
 *       MySQL/MariaDB/PostgreSQL, migrate the data, then re-run this converter.</li>
 * </ul>
 * <p>
 * LimboAuth uses BCrypt exclusively for new registrations. Configure AuthMe with
 * {@code passwordHash: BCRYPT} before running this converter.
 */
public class LimboAuthConverter implements Converter {

    private static final String CONFIG_PATH = "plugins/limboauth/config.yml";
    private static final String SQLITE_DB_PATH = "plugins/limboauth/limboauth.db";
    private static final String TABLE = "AUTH";
    private static final String QUERY =
        "SELECT NICKNAME, LOWERCASENICKNAME, HASH, IP, REGDATE, LOGINDATE, UUID, PREMIUMUUID, TOTPTOKEN FROM " + TABLE;

    private static final ConsoleLogger logger = ConsoleLoggerFactory.get(LimboAuthConverter.class);
    private final DataSource dataSource;

    @Inject
    LimboAuthConverter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void execute(CommandSender sender) {
        File configFile = new File(CONFIG_PATH);
        if (!configFile.exists()) {
            logAndSendMessage(sender, "LimboAuth conversion failed: config not found at " + CONFIG_PATH);
            return;
        }

        YamlConfiguration limboConfig = YamlConfiguration.loadConfiguration(configFile);
        String storageType = limboConfig.getString("database.storage-type", "H2").toUpperCase(Locale.ROOT);

        switch (storageType) {
            case "SQLITE":
                new SqliteImport(dataSource).execute(sender);
                break;
            case "MYSQL":
            case "MARIADB":
            case "POSTGRESQL":
                new SqlImport(dataSource).execute(sender);
                break;
            default:
                logAndSendMessage(sender,
                    "LimboAuth conversion: database type '" + storageType + "' is not supported. "
                    + "Reconfigure LimboAuth to use SQLite or MySQL/MariaDB/PostgreSQL, migrate the data, "
                    + "then re-run the conversion.");
        }
    }

    private static void importRows(ResultSet rs, DataSource dataSource, CommandSender sender) throws SQLException {
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

            if (dataSource.isAuthAvailable(name)) {
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

            PlayerAuth auth = PlayerAuth.builder()
                .name(name)
                .realName(realName)
                .password(new HashedPassword(hash))
                .lastIp(rs.getString("IP"))
                .registrationDate(regDate)
                .lastLogin(loginDate > 0 ? loginDate : null)
                .totpKey(rs.getString("TOTPTOKEN"))
                .uuid(UuidUtils.parseUuidSafely(rs.getString("UUID")))
                .premiumUuid(UuidUtils.parseUuidSafely(rs.getString("PREMIUMUUID")))
                .build();

            dataSource.saveAuth(auth);
            ++imported;
        }

        logAndSendMessage(sender, "LimboAuth conversion: " + imported + " account(s) imported, "
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
                logAndSendMessage(sender,
                    "LimboAuth conversion failed: SQLite database not found at " + SQLITE_DB_PATH);
                return;
            }

            try (Connection conn = openConnection(dbFile);
                 PreparedStatement ps = conn.prepareStatement(QUERY);
                 ResultSet rs = ps.executeQuery()) {
                importRows(rs, getDataSource(), sender);
            } catch (SQLException e) {
                logAndSendMessage(sender, "LimboAuth conversion failed: " + e.getMessage());
                logger.logException("LimboAuth conversion error:", e);
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
                logAndSendMessage(sender, "LimboAuth conversion failed: " + e.getMessage());
                logger.logException("LimboAuth conversion error:", e);
            }
        }
    }
}
