package fr.xephi.authme.commands.dynamic.executable;

import com.timvisee.dungeonmaze.Core;
import com.timvisee.dungeonmaze.command.CommandParts;
import com.timvisee.dungeonmaze.command.ExecutableCommand;
import com.timvisee.dungeonmaze.util.Profiler;
import com.timvisee.dungeonmaze.world.WorldManager;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

public class LoadWorldCommand extends ExecutableCommand {

    /**
     * Execute the command.
     *
     * @param sender The command sender.
     * @param commandReference The command reference.
     * @param commandArguments The command arguments.
     *
     * @return True if the command was executed successfully, false otherwise.
     */
    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // Get and trim the preferred world name
        String worldName = commandArguments.get(0).trim();

        // Profile the world loading
        Profiler p = new Profiler(true);

        // Validate the world name
        if(!WorldManager.isValidWorldName(worldName)) {
            sender.sendMessage(ChatColor.DARK_RED + worldName);
            sender.sendMessage(ChatColor.DARK_RED + "The world name contains invalid characters!");
            return true;
        }

        // Get the world manager, and make sure it's valid
        WorldManager worldManager = Core.getWorldManager();
        boolean showWorldManagerError = false;
        if(worldManager == null)
            showWorldManagerError = true;
        else if(!worldManager.isInit())
            showWorldManagerError = true;
        if(showWorldManagerError) {
            sender.sendMessage(ChatColor.DARK_RED + "Failed to load the world, world manager not available!");
            return true;
        }

        // Make sure the world exists
        if(!worldManager.isWorld(worldName)) {
            sender.sendMessage(ChatColor.DARK_RED + "The world " + ChatColor.GOLD + worldName + ChatColor.DARK_RED + " doesn't exist!");
            return true;
        }

        // Make sure the world isn't loaded already
        if(worldManager.isWorldLoaded(worldName)) {
            sender.sendMessage(ChatColor.GREEN + "The world " + ChatColor.GOLD + worldName + ChatColor.GREEN + " is already loaded!");
            return true;
        }

        // Load the world, store the instance
        World world = worldManager.loadWorld(worldName);

        // Make sure the world was loaded
        if(world == null) {
            sender.sendMessage(ChatColor.DARK_RED + "Failed to load the world!");
            return true;
        }

        // Show a status message, return the result
        sender.sendMessage(ChatColor.GREEN + "The world " + ChatColor.GOLD + worldName + ChatColor.GREEN + " has been loaded, took " + p.getTimeFormatted() + "!");
        return true;
    }
}
