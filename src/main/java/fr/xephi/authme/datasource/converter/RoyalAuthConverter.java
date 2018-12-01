package fr.xephi.authme.datasource.converter;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.player.NamedIdentifier;
import fr.xephi.authme.data.player.OfflineIdentifier;
import fr.xephi.authme.data.player.OnlineIdentifier;
import fr.xephi.authme.datasource.DataSource;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.inject.Inject;
import java.io.File;

import static fr.xephi.authme.util.FileUtils.makePath;

public class RoyalAuthConverter implements Converter {

    private static final String LAST_LOGIN_PATH = "timestamps.quit";
    private static final String PASSWORD_PATH = "login.password";
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
                OfflineIdentifier identifier = new OfflineIdentifier(player);
                File file = new File(makePath(".", "plugins", "RoyalAuth", "userdata", identifier.getLowercaseName() + ".yml"));

                if (dataSource.isAuthAvailable(identifier) || !file.exists()) {
                    continue;
                }
                FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
                PlayerAuth auth = PlayerAuth.builder()
                    .name(identifier.getLowercaseName())
                    .password(configuration.getString(PASSWORD_PATH), null)
                    .lastLogin(configuration.getLong(LAST_LOGIN_PATH))
                    .realName(identifier.getRealName().orElse(null))
                    .build();

                dataSource.saveAuth(auth);
                dataSource.updateSession(auth);
            } catch (Exception e) {
                ConsoleLogger.logException("Error while trying to import " + player.getName() + " RoyalAuth data", e);
            }
        }
    }

}
