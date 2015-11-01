package fr.xephi.authme.commands.dynamic.executable;

import com.timvisee.dungeonmaze.Core;
import com.timvisee.dungeonmaze.DungeonMaze;
import com.timvisee.dungeonmaze.command.CommandParts;
import com.timvisee.dungeonmaze.command.ExecutableCommand;
import com.timvisee.dungeonmaze.util.Profiler;
import com.timvisee.dungeonmaze.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateWorldCommand extends ExecutableCommand {

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

        // Validate the world name
        if(!WorldManager.isValidWorldName(worldName)) {
            sender.sendMessage(ChatColor.DARK_RED + worldName);
            sender.sendMessage(ChatColor.DARK_RED + "The world name contains invalid characters!");
            return true;
        }

        // Set whether the world should be preloaded
        boolean preloadWorld = true;

        // Get whether the world should be preloaded based on the arguments
        if(commandArguments.getCount() >= 2) {
            String arg = commandArguments.get(1);

            // Check whether the argument equals 'force'
            if(arg.equalsIgnoreCase("preload"))
                preloadWorld = true;

            else if(arg.equalsIgnoreCase("true") || arg.equalsIgnoreCase("t") || arg.equalsIgnoreCase("yes") || arg.equalsIgnoreCase("y"))
                preloadWorld = true;

            else if(arg.equalsIgnoreCase("false") || arg.equalsIgnoreCase("f") || arg.equalsIgnoreCase("no") || arg.equalsIgnoreCase("n"))
                preloadWorld = false;

            else {
                sender.sendMessage(ChatColor.DARK_RED + arg);
                sender.sendMessage(ChatColor.DARK_RED + "Invalid argument!");
                return true;
            }
        }

        // Get the world manager, and make sure it's valid
        WorldManager worldManager = Core.getWorldManager();
        boolean showWorldManagerError = false;
        if(worldManager == null)
            showWorldManagerError = true;
        else if(!worldManager.isInit())
            showWorldManagerError = true;
        if(showWorldManagerError) {
            sender.sendMessage(ChatColor.DARK_RED + "Failed to create the world, world manager not available!");
            return true;
        }

        // Make sure the world doesn't exist
        if(worldManager.isWorld(worldName)) {
            sender.sendMessage(ChatColor.DARK_RED + "The world " + ChatColor.GOLD + worldName + ChatColor.DARK_RED + " already exists!");
            sender.sendMessage(ChatColor.YELLOW + "Use the command " + ChatColor.GOLD + "/" + commandReference.get(0) + " listworlds" + ChatColor.YELLOW + " to list all worlds.");
            sender.sendMessage(ChatColor.YELLOW + "Use the command " + ChatColor.GOLD + "/" + commandReference.get(0) + " loadworld " + worldName + ChatColor.YELLOW + " to load the world.");
            return true;
        }

        // Show a status message
        sender.sendMessage(ChatColor.YELLOW + "Preparing the server...");

        // Prepare the server for the new world
        if(!worldManager.prepareDungeonMazeWorld(worldName, preloadWorld)) {
            sender.sendMessage(ChatColor.DARK_RED + "Failed to prepare the server!");
            return true;
        }

        // Show a status message
        sender.sendMessage(ChatColor.YELLOW + "Generating the " + DungeonMaze.PLUGIN_NAME + " " + ChatColor.GOLD + worldName + ChatColor.YELLOW + "...");
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "Generating a new world, expecting lag for a while...");

        // Profile the world generation
        Profiler p = new Profiler(true);

        // Create the world
        // TODO: Put this in a separate function!
        WorldCreator newWorld = new WorldCreator(worldName);
        newWorld.generator(DungeonMaze.instance.getDungeonMazeGenerator());
        World world = newWorld.createWorld();

        // Force-save the level.dat file for the world, profile the process
        Profiler pWorldSave = new Profiler(true);
        try {
            // Force-save the world, and show some status messages
            Core.getLogger().info("Force saving the level.dat file for '" + world.getName() + "'...");
            world.save();
            Core.getLogger().info("World saved successfully, took " + pWorldSave.getTimeFormatted() + "!");

        } catch(Exception ex) {
            Core.getLogger().error("Failed to save the world after " + pWorldSave.getTimeFormatted() + "!");
        }

        // Make sure the world instance is valid
        if(world == null) {
            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "World generation failed after " + p.getTimeFormatted() + "!");
            sender.sendMessage(ChatColor.DARK_RED + "The " + DungeonMaze.PLUGIN_NAME + " " + ChatColor.GOLD + worldName + ChatColor.GREEN +
                    " failed to generate after " + p.getTimeFormatted() + "!");
            return true;
        }

        // Show a status message
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "World generation finished, took " + p.getTimeFormatted() + "!");
        sender.sendMessage(ChatColor.GREEN + "The " + DungeonMaze.PLUGIN_NAME + " " + ChatColor.GOLD + worldName + ChatColor.GREEN +
                " has successfully been generated, took " + p.getTimeFormatted() + "!");

        // If the command was executed by a player, teleport the player
        if(sender instanceof Player) {
            // Teleport the player
            ((Player) sender).teleport(world.getSpawnLocation());
            sender.sendMessage(ChatColor.GREEN + "You have been teleported to " + ChatColor.GOLD + worldName + ChatColor.GREEN + "!");
        }

        // Return the result
        return true;
    }
}
