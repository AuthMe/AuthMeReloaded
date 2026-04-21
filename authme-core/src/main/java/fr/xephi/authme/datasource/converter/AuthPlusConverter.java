package fr.xephi.authme.datasource.converter;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.inject.Inject;
import java.io.File;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static fr.xephi.authme.util.Utils.logAndSendMessage;

/**
 * Converts data from Auth+ to AuthMe.
 * <p>
 * Auth+ stores accounts in {@code plugins/Auth/players.yml} as UUID keys with a nested {@code hash} field
 * using PBKDF2-HmacSHA256 (Base64-encoded salt and hash). Set {@code passwordHash: PBKDF2BASE64} in
 * AuthMe's config before running this converter.
 */
public class AuthPlusConverter implements Converter {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(AuthPlusConverter.class);
    private final DataSource dataSource;
    private final File authPlusFile;

    @Inject
    AuthPlusConverter(@DataFolder File dataFolder, DataSource dataSource) {
        this.dataSource = dataSource;
        this.authPlusFile = new File(dataFolder.getParentFile(), "Auth/players.yml");
    }

    @Override
    public void execute(CommandSender sender) {
        if (!authPlusFile.exists()) {
            sender.sendMessage("The file '" + authPlusFile.getPath() + "' does not exist");
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(authPlusFile);
        Set<String> keys = config.getKeys(false);

        long successCount = 0;
        long skippedCount = 0;
        for (String uuidStr : keys) {
            String hash = config.getString(uuidStr + ".hash");
            if (hash == null || hash.isEmpty()) {
                logger.warning("No hash found for UUID '" + uuidStr + "', skipping");
                continue;
            }

            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid UUID '" + uuidStr + "', skipping");
                continue;
            }

            String name = resolveName(uuid);
            if (name == null) {
                logger.warning("Could not resolve name for UUID '" + uuidStr + "', skipping");
                continue;
            }

            String lowercaseName = name.toLowerCase(Locale.ROOT);
            if (dataSource.isAuthAvailable(lowercaseName)) {
                ++skippedCount;
                continue;
            }

            PlayerAuth auth = PlayerAuth.builder()
                .name(lowercaseName)
                .realName(name)
                .uuid(uuid)
                .password(hash, null)
                .build();
            dataSource.saveAuth(auth);
            ++successCount;
        }

        logAndSendMessage(sender, "Auth+ conversion: " + successCount + " account(s) imported, "
            + skippedCount + " skipped (already exist)");
    }

    private String resolveName(UUID uuid) {
        try {
            return Bukkit.getOfflinePlayer(uuid).getName();
        } catch (Exception | NoSuchMethodError e) {
            for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                if (uuid.equals(op.getUniqueId())) {
                    return op.getName();
                }
            }
            return null;
        }
    }
}
