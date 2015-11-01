package fr.xephi.authme.commands.dynamic.executable;

import com.timvisee.dungeonmaze.Core;
import com.timvisee.dungeonmaze.command.CommandParts;
import com.timvisee.dungeonmaze.command.ExecutableCommand;
import com.timvisee.dungeonmaze.util.Profiler;
import com.timvisee.dungeonmaze.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class UnloadWorldCommand extends ExecutableCommand {

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

        // Profile the world unloading
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
            sender.sendMessage(ChatColor.DARK_RED + "Failed to unload the world, world manager not available!");
            return true;
        }

        // Make sure the world exists
        if(!worldManager.isWorld(worldName)) {
            sender.sendMessage(ChatColor.DARK_RED + "The world " + ChatColor.GOLD + worldName + ChatColor.DARK_RED + " doesn't exist!");
            return true;
        }

        // Make sure the world is loaded
        if(!worldManager.isWorldLoaded(worldName)) {
            sender.sendMessage(ChatColor.DARK_RED + "The world " + ChatColor.GOLD + worldName + ChatColor.DARK_RED + " isn't loaded!");
            return true;
        }

        // Make sure the main world isn't unloaded
        if(worldManager.isMainWorld(worldName)) {
            sender.sendMessage(ChatColor.DARK_RED + "The main world can't be unloaded!");
            return true;
        }

        // Get all players in the world
        List<Player> players = Bukkit.getOnlinePlayers().stream().filter(player -> player.getWorld().getName().equals(worldName)).collect(Collectors.toList());
        int playerCount = players.size();

        // Teleport all players away
        if(playerCount > 0) {
            // Get the main world
            World mainWorld = worldManager.getMainWorld();
            Location mainWorldSpawn = mainWorld.getSpawnLocation();

            // Teleport all players
            for(Player player : players) {
                // Teleport the player to the spawn of the main world
                player.teleport(mainWorldSpawn);

                // Show a message to the player
                player.sendMessage(ChatColor.YELLOW + "The current world is being unloaded, you've been teleported!");
            }

            // Show a status message
            sender.sendMessage(ChatColor.YELLOW + "Teleported " + ChatColor.GOLD + playerCount + ChatColor.YELLOW + " player" + (playerCount != 1 ? "s" : "") + " away!");
        }

        // Force the world to be loaded if it isn't already loaded
        if(!worldManager.unloadWorld(worldName)) {
            sender.sendMessage(ChatColor.DARK_RED + "Failed to unload the world!");
            return true;
        }

        // Show a status message, return the result
        sender.sendMessage(ChatColor.GREEN + "The world " + ChatColor.GOLD + worldName + ChatColor.GREEN + " has been unloaded, took " + p.getTimeFormatted() + "!");
        return true;
    }
}
