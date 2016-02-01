package fr.xephi.authme.converter;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import org.bukkit.OfflinePlayer;

import java.io.File;

public class RoyalAuthConverter implements Converter {

    private final AuthMe plugin;
    private final DataSource data;

    public RoyalAuthConverter(AuthMe plugin) {
        this.plugin = plugin;
        this.data = plugin.getDataSource();
    }

    @Override
    public void run() {
        for (OfflinePlayer o : plugin.getServer().getOfflinePlayers()) {
            try {
                String name = o.getName().toLowerCase();
                String sp = File.separator;
                File file = new File("." + sp + "plugins" + sp + "RoyalAuth" + sp + "userdata" + sp + name + ".yml");
                if (data.isAuthAvailable(name))
                    continue;
                if (!file.exists())
                    continue;
                RoyalAuthYamlReader ra = new RoyalAuthYamlReader(file);
                PlayerAuth auth = new PlayerAuth(name, ra.getHash(), "127.0.0.1", ra.getLastLogin(), "your@email.com", o.getName());
                data.saveAuth(auth);
            } catch (Exception e) {
                ConsoleLogger.writeStackTrace(e);
                ConsoleLogger.showError("Error while trying to import " + o.getName() + " RoyalAuth datas");
            }
        }
    }

}
