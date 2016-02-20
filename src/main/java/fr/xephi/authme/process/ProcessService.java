package fr.xephi.authme.process;

import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.domain.Property;
import org.bukkit.command.CommandSender;

/**
 * Service for asynchronous and synchronous processes.
 */
public class ProcessService {

    private final NewSetting settings;
    private final Messages messages;

    public ProcessService(NewSetting settings, Messages messages) {
        this.settings = settings;
        this.messages = messages;
    }

    public <T> T getProperty(Property<T> property) {
        return settings.getProperty(property);
    }

    public NewSetting getSettings() {
        return settings;
    }

    public void send(CommandSender sender, MessageKey key) {
        messages.send(sender, key);
    }

}
