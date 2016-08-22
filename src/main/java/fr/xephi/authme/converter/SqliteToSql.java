package fr.xephi.authme.converter;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.datasource.SQLite;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.Settings;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;

public class SqliteToSql implements Converter {

    private final Settings settings;
    private final DataSource dataSource;
    private final Messages messages;

    @Inject
    SqliteToSql(Settings settings, DataSource dataSource, Messages messages) {
        this.settings = settings;
        this.dataSource = dataSource;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender) {
        if (dataSource.getType() != DataSourceType.MYSQL) {
            sender.sendMessage("Please configure your mySQL connection and re-run this command");
            return;
        }
        try {
            SQLite data = new SQLite(settings);
            for (PlayerAuth auth : data.getAllAuths()) {
                dataSource.saveAuth(auth);
            }
        } catch (Exception e) {
            messages.send(sender, MessageKey.ERROR);
            ConsoleLogger.logException("Problem during SQLite to SQL conversion:", e);
        }
    }

}
