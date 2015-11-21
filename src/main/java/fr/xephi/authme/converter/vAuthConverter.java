package fr.xephi.authme.converter;

import org.bukkit.command.CommandSender;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.DataSource;

/**
 */
public class vAuthConverter implements Converter {

    public AuthMe plugin;
    public DataSource database;
    public CommandSender sender;

    /**
     * Constructor for vAuthConverter.
     * @param plugin AuthMe
     * @param sender CommandSender
     */
    public vAuthConverter(AuthMe plugin, CommandSender sender) {
        this.plugin = plugin;
        this.database = plugin.database;
        this.sender = sender;
    }

    /**
     * Method run.
     * @see java.lang.Runnable#run()
     */
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
