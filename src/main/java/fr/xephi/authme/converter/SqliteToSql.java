package fr.xephi.authme.converter;

import java.sql.SQLException;

import org.bukkit.command.CommandSender;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource.DataSourceType;
import fr.xephi.authme.datasource.SQLite;
import fr.xephi.authme.output.MessageKey;

public class SqliteToSql implements Converter {

	private AuthMe plugin;
	private CommandSender sender;

	public SqliteToSql(AuthMe plugin, CommandSender sender) {
		this.plugin = plugin;
		this.sender = sender;
	}

	@Override
	public void run() {
		if (plugin.database.getType() != DataSourceType.MYSQL)
		{
			sender.sendMessage("Please config your mySQL connection and re-run this command");
			return;
		}
		try {
			SQLite data = new SQLite();
			for (PlayerAuth auth : data.getAllAuths())
			{
				plugin.database.saveAuth(auth);
			}
			sender.sendMessage("Convert command has finished !");
		} catch (ClassNotFoundException e) {
			sender.sendMessage(plugin.getMessages().retrieve(MessageKey.ERROR));
			e.printStackTrace();
		} catch (SQLException e) {
			sender.sendMessage(plugin.getMessages().retrieve(MessageKey.ERROR));
			e.printStackTrace();
		}
	}

}
