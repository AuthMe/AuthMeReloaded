package fr.xephi.authme.converter;

import fr.xephi.authme.AuthMe;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;

public class xAuthConverter implements Converter {

    private final AuthMe plugin;

    @Inject
    xAuthConverter(AuthMe plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender) {
        try {
            Class.forName("de.luricos.bukkit.xAuth.xAuth");
            xAuthToFlat converter = new xAuthToFlat(plugin, sender);
            converter.convert();
        } catch (ClassNotFoundException ce) {
            sender.sendMessage("xAuth has not been found, please put xAuth.jar in your plugin folder and restart!");
        }
    }

}
