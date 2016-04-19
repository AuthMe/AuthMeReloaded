package fr.xephi.authme.converter;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

import static fr.xephi.authme.util.StringUtils.makePath;

public class RoyalAuthConverter implements Converter {

    private static final String LAST_LOGIN_PATH = "timestamps.quit";
    private static final String PASSWORD_PATH = "login.password";
    private final AuthMe plugin;
    private final DataSource dataSource;

    public RoyalAuthConverter(AuthMe plugin) {
        this.plugin = plugin;
        this.dataSource = plugin.getDataSource();
    }

    @Override
    public void run() {
        for (OfflinePlayer player : plugin.getServer().getOfflinePlayers()) {
            try {
                String name = player.getName().toLowerCase();
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
            } catch (Exception e) {
                ConsoleLogger.logException("Error while trying to import " + player.getName() + " RoyalAuth data", e);
            }
        }
    }

}
