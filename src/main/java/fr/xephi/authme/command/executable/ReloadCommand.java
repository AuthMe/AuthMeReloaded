package fr.xephi.authme.command.executable;

import com.timvisee.dungeonmaze.Core;
import com.timvisee.dungeonmaze.command.CommandParts;
import com.timvisee.dungeonmaze.command.ExecutableCommand;
import com.timvisee.dungeonmaze.config.ConfigHandler;
import com.timvisee.dungeonmaze.util.Profiler;
import com.timvisee.dungeonmaze.world.WorldManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

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

        // Show a status message
        sender.sendMessage(ChatColor.YELLOW + "Reloading Dungeon Maze...");

        /* // Set whether the reload is forced
        boolean force = false;

        // Get whether the reload should be forced from the command arguments
        if(commandArguments.getCount() >= 1) {
            String arg = commandArguments.get(0);

            // Check whether the argument equals 'force'
            if(arg.equalsIgnoreCase("force") || arg.equalsIgnoreCase("forced"))
                force = true;

            else if(arg.equalsIgnoreCase("true") || arg.equalsIgnoreCase("t") || arg.equalsIgnoreCase("yes") || arg.equalsIgnoreCase("y"))
                force = true;

            else if(arg.equalsIgnoreCase("false") || arg.equalsIgnoreCase("f") || arg.equalsIgnoreCase("no") || arg.equalsIgnoreCase("n"))
                force = false;

            else {
                sender.sendMessage(ChatColor.DARK_RED + arg);
                sender.sendMessage(ChatColor.DARK_RED + "Invalid argument!");
                return true;
            }
        }*/

        // Reload the configuration
        ConfigHandler configHandler = Core.getConfigHandler();
        if(configHandler != null) {
            configHandler.load();
            sender.sendMessage(ChatColor.YELLOW + "Reloaded the configuration!");
        } else
            sender.sendMessage(ChatColor.DARK_RED + "Failed to reload the configuration!");

        // Get the world manager to reload the world list, and make sure it's valid
        WorldManager worldManager = Core.getWorldManager();
        if(worldManager != null) {
            worldManager.refresh();
            sender.sendMessage(ChatColor.YELLOW + "Reloaded the worlds!");
        } else
            sender.sendMessage(ChatColor.DARK_RED + "Failed to reload the worlds!");

        // Dungeon Maze reloaded, show a status message
        sender.sendMessage(ChatColor.GREEN + "Dungeon Maze has been reloaded successfully, took " + p.getTimeFormatted() + "!");
        return true;
    }
}
