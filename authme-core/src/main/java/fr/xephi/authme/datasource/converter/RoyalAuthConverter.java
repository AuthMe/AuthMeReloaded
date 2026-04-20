package fr.xephi.authme.datasource.converter;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.inject.Inject;
import java.io.File;
import java.util.Locale;

import static fr.xephi.authme.util.FileUtils.makePath;

public class RoyalAuthConverter implements Converter {

    private static final String LAST_LOGIN_PATH = "timestamps.quit";
    private static final String PASSWORD_PATH = "login.password";

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(RoyalAuthConverter.class);

    private final AuthMe plugin;
    private final DataSource dataSource;

    @Inject
    RoyalAuthConverter(AuthMe plugin, DataSource dataSource) {
        this.plugin = plugin;
        this.dataSource = dataSource;
    }

    @Override
    public void execute(CommandSender sender) {
        for (OfflinePlayer player : plugin.getServer().getOfflinePlayers()) {
            try {
                String name = player.getName().toLowerCase(Locale.ROOT);
                File file = new File(makePath(".", "plugins", "RoyalAuth", "userdata", name + ".yml"));

                if (dataSource.isAuthAvailable(name) || !file.exists()) {
                    continue;
                }
                FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
                PlayerAuth auth = PlayerAuth.builder()
                    .name(name)
                    .password(configuration.getString(PASSWORD_PATH), null)
                    .lastLogin(configuration.getLong(LAST_LOGIN_PATH))
                    .realName(player.getName())
                    .build();

                dataSource.saveAuth(auth);
                dataSource.updateSession(auth);
            } catch (Exception e) {
                logger.logException("Error while trying to import " + player.getName() + " RoyalAuth data", e);
            }
        }
    }

}
