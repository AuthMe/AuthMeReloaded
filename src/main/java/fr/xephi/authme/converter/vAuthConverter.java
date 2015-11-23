package fr.xephi.authme.converter;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.DataSource;
import org.bukkit.command.CommandSender;

/**
 */
public class vAuthConverter implements Converter {

    public final AuthMe plugin;
    public final DataSource database;
    public final CommandSender sender;

    /**
     * Constructor for vAuthConverter.
     *
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
     *
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
