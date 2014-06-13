package fr.xephi.authme.converter;

import org.bukkit.command.CommandSender;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.datasource.DataSource;

public class xAuthConverter implements Converter {

    public AuthMe plugin;
    public DataSource database;
    public CommandSender sender;
    
    public xAuthConverter(AuthMe plugin, DataSource database, CommandSender sender) {
        this.plugin = plugin;
        this.database = database;
        this.sender = sender;
    }
    @Override
    public void convert() throws Exception {
        try {
            Class.forName("com.cypherx.xauth.xAuth");
            oldxAuthToFlat converter = new oldxAuthToFlat(plugin, database, sender);
            converter.run();
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("de.luricos.bukkit.xAuth.xAuth");
                newxAuthToFlat converter = new newxAuthToFlat(plugin, database, sender);
                converter.run();
            } catch (ClassNotFoundException ce) {
            }
        }
    }

}
