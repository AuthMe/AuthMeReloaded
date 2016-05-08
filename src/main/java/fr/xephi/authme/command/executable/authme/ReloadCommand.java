package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;

/**
 * The reload command.
 */
public class ReloadCommand implements ExecutableCommand {

    @Inject
    private AuthMe plugin;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        try {
            plugin.reload();
            commandService.send(sender, MessageKey.CONFIG_RELOAD_SUCCESS);
        } catch (Exception e) {
            sender.sendMessage("Error occurred during reload of AuthMe: aborting");
            ConsoleLogger.logException("Aborting! Encountered exception during reload of AuthMe:", e);
            plugin.stopOrUnload();
        }
    }
}
