package fr.xephi.authme.command.executable;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;

import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.Profiler;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ReloadCommand extends ExecutableCommand {

    /** AuthMe plugin instance. */
    private AuthMe plugin = AuthMe.getInstance();
    /** Messages instance. */
    private Messages m = Messages.getInstance();

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

        // Show a status message
        sender.sendMessage(ChatColor.YELLOW + "Reloading Dungeon Maze...");

        try {
            Settings.reload();
            plugin.getModuleManager().reloadModules();
            m.reloadMessages();
            plugin.setupDatabase();
        } catch (Exception e) {
            ConsoleLogger.showError("Fatal error occurred! Authme instance ABORTED!");
            ConsoleLogger.writeStackTrace(e);
            plugin.stopOrUnload();
            return false;
        }
        m.send(sender, "reload");

        // Dungeon Maze reloaded, show a status message
        sender.sendMessage(ChatColor.GREEN + "Dungeon Maze has been reloaded successfully, took " + p.getTimeFormatted() + "!");
        return true;
    }
}
