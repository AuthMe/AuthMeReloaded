package fr.xephi.authme.converter;

import fr.xephi.authme.settings.NewSetting;
import org.bukkit.command.CommandSender;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.datasource.SQLite;
import fr.xephi.authme.output.MessageKey;

public class SqliteToSql implements Converter {

    private final AuthMe plugin;
    private final CommandSender sender;
    private final NewSetting settings;

    public SqliteToSql(AuthMe plugin, CommandSender sender, NewSetting settings) {
        this.plugin = plugin;
        this.sender = sender;
        this.settings = settings;
    }

    @Override
    public void run() {
        if (plugin.getDataSource().getType() != DataSourceType.MYSQL) {
            sender.sendMessage("Please configure your mySQL connection and re-run this command");
            return;
        }
        try {
            SQLite data = new SQLite(settings);
            for (PlayerAuth auth : data.getAllAuths()) {
                plugin.getDataSource().saveAuth(auth);
            }
        } catch (Exception e) {
            plugin.getMessages().send(sender, MessageKey.ERROR);
            ConsoleLogger.logException("Problem during SQLite to SQL conversion:", e);
        }
    }

}
