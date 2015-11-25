package fr.xephi.authme.command.executable.authme;

//import org.bukkit.ChatColor;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.Profiler;
import org.bukkit.command.CommandSender;

/**
 */
public class ReloadCommand extends ExecutableCommand {

    /**
     * Execute the command.
     *
     * @param sender           The command sender.
     * @param commandReference The command reference.
     * @param commandArguments The command arguments.
     *
     * @return True if the command was executed successfully, false otherwise.
     */
    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // Profile the reload process
        Profiler p = new Profiler(true);

        // AuthMe plugin instance
        AuthMe plugin = AuthMe.getInstance();

        // Messages instance
        Messages m = Messages.getInstance();

        // Show a status message
        // sender.sendMessage(ChatColor.YELLOW + "Reloading AuthMeReloaded...");

        try {
            Settings.reload();
            plugin.getModuleManager().reloadModules();
            Messages.getInstance().reloadMessages();
            plugin.setupDatabase();
        } catch (Exception e) {
            ConsoleLogger.showError("Fatal error occurred! AuthMe instance ABORTED!");
            ConsoleLogger.writeStackTrace(e);
            plugin.stopOrUnload();
            return false;
        }

        // Show a status message
        // TODO: add the profiler result
        m.send(sender, "reload");

        // AuthMeReloaded reloaded, show a status message
        // sender.sendMessage(ChatColor.GREEN + "AuthMeReloaded has been reloaded successfully, took " + p.getTimeFormatted() + "!");
        return true;
    }
}
