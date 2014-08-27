package fr.xephi.authme.converter;

import java.io.File;

import org.bukkit.OfflinePlayer;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;

public class RoyalAuthConverter implements Converter {

    public AuthMe plugin;
    private DataSource data;

    public RoyalAuthConverter(AuthMe plugin) {
        this.plugin = plugin;
        this.data = plugin.database;
    }

    @Override
    public void run() {
        for (OfflinePlayer o : plugin.getServer().getOfflinePlayers()) {
            try {
                String name = o.getName().toLowerCase();
                String separator = File.separator;
                File file = new File("." + separator + "plugins" + separator + "RoyalAuth" + separator + "userdata" + separator + name + ".yml");
                if (data.isAuthAvailable(name))
                    continue;
                if (!file.exists())
                    continue;
                RoyalAuthYamlReader ra = new RoyalAuthYamlReader(file);
                PlayerAuth auth = new PlayerAuth(o.getName(), ra.getHash(), "127.0.0.1", ra.getLastLogin(), "your@email.com");
                data.saveAuth(auth);
            } catch (Exception e) {
                ConsoleLogger.showError("Error while trying to import " + o.getName() + " RoyalAuth datas");
            }
        }
    }

}
