package fr.xephi.authme.converter;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.FlatFile;
import fr.xephi.authme.settings.Messages;
import org.bukkit.command.CommandSender;

import java.util.List;

public class SqlToFlat implements Converter {

    public AuthMe plugin;
    public DataSource database;
    public CommandSender sender;

    public SqlToFlat(AuthMe plugin, CommandSender sender) {
        this.plugin = plugin;
        this.database = plugin.database;
        this.sender = sender;
    }

    @Override
    public void run() {
        try {
            FlatFile flat = new FlatFile();
            List<PlayerAuth> auths = database.getAllAuths();
            int i = 0;
            final int size = auths.size();
            for (PlayerAuth auth : auths) {
                flat.saveAuth(auth);
                i++;
                if ((i % 100) == 0) {
                    sender.sendMessage("Conversion Status : " + i + " / " + size);
                }
            }
            sender.sendMessage("Successfully convert from SQL table to file auths.db");
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            Messages.getInstance().send(sender, "error");
        }
    }

}
