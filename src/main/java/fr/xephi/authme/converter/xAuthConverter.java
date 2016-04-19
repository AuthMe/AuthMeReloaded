package fr.xephi.authme.converter;

import fr.xephi.authme.AuthMe;
import org.bukkit.command.CommandSender;

public class xAuthConverter implements Converter {

    private final AuthMe plugin;
    private final CommandSender sender;

    public xAuthConverter(AuthMe plugin, CommandSender sender) {
        this.plugin = plugin;
        this.sender = sender;
    }

    @Override
    public void run() {
        try {
            Class.forName("de.luricos.bukkit.xAuth.xAuth");
            xAuthToFlat converter = new xAuthToFlat(plugin, sender);
            converter.convert();
        } catch (ClassNotFoundException ce) {
            sender.sendMessage("xAuth has not been found, please put xAuth.jar in your plugin folder and restart!");
        }
    }

}
