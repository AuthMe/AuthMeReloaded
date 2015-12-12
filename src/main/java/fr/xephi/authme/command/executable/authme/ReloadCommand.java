package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.Profiler;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ReloadCommand extends ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        // Profile the reload process
        Profiler p = new Profiler(true);

        // AuthMe plugin instance
        AuthMe plugin = AuthMe.getInstance();

        // Messages instance
        Messages m = plugin.getMessages();

        // Show a status message
        // sender.sendMessage(ChatColor.YELLOW + "Reloading AuthMeReloaded...");

        try {
            Settings.reload();
            Messages.getInstance().reloadManager();
            plugin.getModuleManager().reloadModules();
            plugin.setupDatabase();
        } catch (Exception e) {
            ConsoleLogger.showError("Fatal error occurred! AuthMe instance ABORTED!");
            ConsoleLogger.writeStackTrace(e);
            plugin.stopOrUnload();
        }

        // Show a status message
        // TODO: add the profiler result
        m.send(sender, MessageKey.CONFIG_RELOAD_SUCCESS);

        // AuthMeReloaded reloaded, show a status message
        // sender.sendMessage(ChatColor.GREEN + "AuthMeReloaded has been reloaded successfully, took " + p.getTimeFormatted() + "!");
    }
}
