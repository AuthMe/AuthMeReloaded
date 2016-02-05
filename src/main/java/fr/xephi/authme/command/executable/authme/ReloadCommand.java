package fr.xephi.authme.command.executable.authme;

import java.util.List;

import org.bukkit.command.CommandSender;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.Settings;

public class ReloadCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        // AuthMe plugin instance
        AuthMe plugin = AuthMe.getInstance();

        try {
            Settings.reload();
            Messages.getInstance().reloadManager();
            plugin.getModuleManager().reloadModules();
            plugin.setupDatabase();
        } catch (Exception e) {
            sender.sendMessage("Error occurred during reload of AuthMe: aborting");
            ConsoleLogger.logException("Aborting! Encountered exception during reload of AuthMe:", e);
            plugin.stopOrUnload();
        }

        commandService.send(sender, MessageKey.CONFIG_RELOAD_SUCCESS);
    }
}
