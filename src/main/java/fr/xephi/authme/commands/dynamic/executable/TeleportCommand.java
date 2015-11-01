package fr.xephi.authme.commands.dynamic.executable;

import com.timvisee.dungeonmaze.Core;
import com.timvisee.dungeonmaze.command.CommandParts;
import com.timvisee.dungeonmaze.command.ExecutableCommand;
import com.timvisee.dungeonmaze.world.WorldManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeleportCommand extends ExecutableCommand {

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
        // Make sure the command is executed by an in-game player
        if(!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.DARK_RED + "You need to be in-game to use this command!");
            return true;
        }

        // Get the player and the world name to teleport to
        Player player = (Player) sender;
        String worldName = commandArguments.get(0);

        // Get the world manager, and make sure it's valid
        WorldManager worldManager = Core.getWorldManager();
        boolean showWorldManagerError = false;
        if(worldManager == null)
            showWorldManagerError = true;
        else if(!worldManager.isInit())
            showWorldManagerError = true;
        if(showWorldManagerError) {
            sender.sendMessage(ChatColor.DARK_RED + "Failed to teleport, world manager not available!");
            return true;
        }

        // Make sure the world exists
        if(!worldManager.isWorld(worldName)) {
            sender.sendMessage(ChatColor.DARK_RED + worldName);
            sender.sendMessage(ChatColor.DARK_RED + "This world doesn't exists!");
            return true;
        }

        // Try to load the world
        World world = worldManager.loadWorld(worldName);

        // Make sure the world was loaded successfully
        if(world == null) {
            sender.sendMessage(ChatColor.DARK_RED + "Failed to teleport, unable to load the world!");
            return true;
        }

        // Get the spawn location to teleport the player to
        Location spawn = world.getSpawnLocation();

        // Force-set the location on Dungeon Maze worlds
        // TODO: Fix this!
        if(worldManager.isDungeonMazeWorld(worldName)) {
            spawn.setX(4);
            spawn.setY(68);
            spawn.setZ(4);
        }

        // Teleport the player, show a status message and return true
        player.teleport(spawn);
        player.sendMessage(ChatColor.GREEN + "You have been teleported to " + ChatColor.GOLD + worldName + ChatColor.GREEN + "!");
        return true;
    }
}
