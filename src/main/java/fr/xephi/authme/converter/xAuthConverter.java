package fr.xephi.authme.converter;

import org.bukkit.command.CommandSender;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.datasource.DataSource;

public class xAuthConverter implements Converter {

    public AuthMe plugin;
    public DataSource database;
    public CommandSender sender;

    public xAuthConverter(AuthMe plugin, DataSource database,
            CommandSender sender) {
        this.plugin = plugin;
        this.database = database;
        this.sender = sender;
    }

    @Override
    public void run() {
        try {
            Class.forName("com.cypherx.xauth.xAuth");
            oldxAuthToFlat converter = new oldxAuthToFlat(plugin, database, sender);
            converter.convert();
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("de.luricos.bukkit.xAuth.xAuth");
                newxAuthToFlat converter = new newxAuthToFlat(plugin, database, sender);
                converter.convert();
            } catch (ClassNotFoundException ce) {
                sender.sendMessage("xAuth has not been found, please put xAuth.jar in your plugin folder and restart!");
            }
        }
    }

}
