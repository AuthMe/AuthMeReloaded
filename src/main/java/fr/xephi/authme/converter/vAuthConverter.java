package fr.xephi.authme.converter;

import org.bukkit.command.CommandSender;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.DataSource;

public class vAuthConverter implements Converter {

    public AuthMe plugin;
    public DataSource database;
    public CommandSender sender;

    public vAuthConverter(AuthMe plugin, DataSource database,
            CommandSender sender) {
        this.plugin = plugin;
        this.database = database;
        this.sender = sender;
    }

    @Override
    public void run() {
        try {
            new vAuthFileReader(plugin, database, sender).convert();
        } catch (Exception e) {
            sender.sendMessage(e.getMessage());
            ConsoleLogger.showError(e.getMessage());
        }
    }

}
