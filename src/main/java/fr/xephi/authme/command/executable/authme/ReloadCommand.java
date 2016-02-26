package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.settings.Spawn;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * The reload command.
 */
public class ReloadCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        AuthMe plugin = commandService.getAuthMe();
        try {
            commandService.getSettings().reload();
            commandService.reloadMessages(commandService.getSettings().getMessagesFile());
            Spawn.reload();
            // TODO #432: We should not reload only certain plugin entities but actually reinitialize all elements,
            // i.e. here in the future we might not have setupDatabase() but Authme.onEnable(), maybe after
            // a call to some destructor method
            plugin.setupDatabase(commandService.getSettings());
            commandService.send(sender, MessageKey.CONFIG_RELOAD_SUCCESS);
        } catch (Exception e) {
            sender.sendMessage("Error occurred during reload of AuthMe: aborting");
            ConsoleLogger.logException("Aborting! Encountered exception during reload of AuthMe:", e);
            plugin.stopOrUnload();
        }
    }
}
