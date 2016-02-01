package fr.xephi.authme.converter;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import org.bukkit.command.CommandSender;

public class vAuthConverter implements Converter {

    private final AuthMe plugin;
    private final CommandSender sender;

    public vAuthConverter(AuthMe plugin, CommandSender sender) {
        this.plugin = plugin;
        this.sender = sender;
    }

    @Override
    public void run() {
        try {
            new vAuthFileReader(plugin).convert();
        } catch (Exception e) {
            sender.sendMessage(e.getMessage());
            ConsoleLogger.showError(e.getMessage());
        }
    }

}
