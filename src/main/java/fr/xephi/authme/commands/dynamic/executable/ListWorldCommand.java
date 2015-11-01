package fr.xephi.authme.commands.dynamic.executable;

import com.timvisee.dungeonmaze.Core;
import com.timvisee.dungeonmaze.DungeonMaze;
import com.timvisee.dungeonmaze.command.CommandParts;
import com.timvisee.dungeonmaze.command.ExecutableCommand;
import com.timvisee.dungeonmaze.world.WorldManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ListWorldCommand extends ExecutableCommand {

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
        // Get the world manager and make sure it's valid
        WorldManager worldManager = Core.getWorldManager();
        if(worldManager == null) {
            sender.sendMessage(ChatColor.DARK_RED + "Error, failed to list the worlds!");
            return true;
        }

        // Get the list of Dungeon Maze worlds and other worlds
        List<String> dungeonMazeWorlds = worldManager.getDungeonMazeWorlds();
        List<String> otherWorlds = worldManager.getWorlds(true);

        // Show the list of Dungeon Maze worlds
        sender.sendMessage(ChatColor.GOLD + "==========[ \" + DungeonMaze.PLUGIN_NAME.toUpperCase() + \" WORLDS ]==========");
        sender.sendMessage(ChatColor.GOLD + DungeonMaze.PLUGIN_NAME + " worlds:");
        if(dungeonMazeWorlds.size() > 0) {
            for(String worldName : dungeonMazeWorlds) {
                if(worldManager.isDungeonMazeWorldLoaded(worldName))
                    sender.sendMessage(ChatColor.WHITE + " " + worldName + ChatColor.GREEN + ChatColor.ITALIC + " (Loaded)");
                else
                    sender.sendMessage(ChatColor.WHITE + " " + worldName + ChatColor.GRAY + ChatColor.ITALIC + " (Not Loaded)");
            }
        } else
            // No Dungeon Maze world available, show a message
            sender.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + " No Dungeon Maze worlds available!");

        // Show the list of other worlds
        sender.sendMessage(ChatColor.GOLD + "Other worlds:");
        if(otherWorlds.size() > 0) {
            for(String worldName : otherWorlds) {
                if(worldManager.isWorldLoaded(worldName))
                    sender.sendMessage(ChatColor.WHITE + " " + worldName + ChatColor.GREEN + ChatColor.ITALIC + " (Loaded)");
                else
                    sender.sendMessage(ChatColor.WHITE + " " + worldName + ChatColor.GRAY + ChatColor.ITALIC + " (Not Loaded)");
            }
        } else
            // No other world available, show a message
            sender.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + " No other worlds available!");

        // Return the result
        return true;
    }
}
