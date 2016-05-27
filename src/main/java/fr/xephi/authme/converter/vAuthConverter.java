package fr.xephi.authme.converter;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;

public class vAuthConverter implements Converter {

    private final AuthMe plugin;

    @Inject
    vAuthConverter(AuthMe plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender) {
        try {
            new vAuthFileReader(plugin).convert();
        } catch (Exception e) {
            sender.sendMessage(e.getMessage());
            ConsoleLogger.showError(e.getMessage());
        }
    }

}
