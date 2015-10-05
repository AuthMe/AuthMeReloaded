package fr.xephi.authme.converter;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.DataSource;
import org.bukkit.command.CommandSender;

public class vAuthConverter implements Converter {

    public AuthMe plugin;
    public DataSource database;
    public CommandSender sender;

    public vAuthConverter(AuthMe plugin, CommandSender sender) {
        this.plugin = plugin;
        this.database = plugin.database;
        this.sender = sender;
    }

    @Override
    public void run() {
        try {
            new vAuthFileReader(plugin, sender).convert();
        } catch (Exception e) {
            sender.sendMessage(e.getMessage());
            ConsoleLogger.showError(e.getMessage());
        }
    }

}
