package fr.xephi.authme.command.executable;

import com.timvisee.dungeonmaze.DungeonMaze;
import com.timvisee.dungeonmaze.command.CommandParts;
import com.timvisee.dungeonmaze.command.ExecutableCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VersionCommand extends ExecutableCommand {

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
        // Show some version info
        sender.sendMessage(ChatColor.GOLD + "==========[ " + DungeonMaze.PLUGIN_NAME.toUpperCase() + " ABOUT ]==========");
        sender.sendMessage(ChatColor.GOLD + "Version: " + ChatColor.WHITE + DungeonMaze.PLUGIN_NAME + " v" + DungeonMaze.getVersionName() + ChatColor.GRAY + " (code: " + DungeonMaze.getVersionCode() + ")");
        sender.sendMessage(ChatColor.GOLD + "Developers:");
        printDeveloper(sender, "Tim Visee", "timvisee", "Lead Developer");
        printDeveloper(sender, "Xephi", "xephi", "Code Contributor");
        printDeveloper(sender, "sgdc3", "sgdc3", "Code Contributor");
        printDeveloper(sender, "Metonymia", "Metonymia", "Design Contributor");
        sender.sendMessage(ChatColor.GOLD + "Website: " + ChatColor.WHITE + "http://timvisee.com/projects/bukkit/dungeon-maze/");
        sender.sendMessage(ChatColor.GOLD + "License: " + ChatColor.WHITE + "GNU GPL v3.0" + ChatColor.GRAY + ChatColor.ITALIC + " (See LICENSE file)");
        sender.sendMessage(ChatColor.GOLD + "Copyright: " + ChatColor.WHITE + "Copyright (c) Tim Visee 2015. All rights reserved.");
        return true;
    }

    /**
     * Print a developer with proper styling.
     *
     * @param sender The command sender.
     * @param name The display name of the developer.
     * @param minecraftName The Minecraft username of the developer, if available.
     * @param function The function of the developer.
     */
    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    private void printDeveloper(CommandSender sender, String name, String minecraftName, String function) {
        // Print the name
        StringBuilder msg = new StringBuilder();
        msg.append(" " + ChatColor.WHITE);
        msg.append(name);

        // Append the Minecraft name, if available
        if(minecraftName.length() != 0)
            msg.append(ChatColor.GRAY + " // " + ChatColor.WHITE + minecraftName);
        msg.append(ChatColor.GRAY + "" + ChatColor.ITALIC + " (" + function + ")");

        // Show the online status
        if(minecraftName.length() != 0)
            if(isPlayerOnline(minecraftName))
                msg.append(ChatColor.GREEN + "" + ChatColor.ITALIC + " (In-Game)");

        // Print the message
        sender.sendMessage(msg.toString());
    }

    /**
     * Check whether a player is online.
     *
     * @param minecraftName The Minecraft player name.
     *
     * @return True if the player is online, false otherwise.
     */
    private boolean isPlayerOnline(String minecraftName) {
        for(Player player : Bukkit.getOnlinePlayers())
            if(player.getName().equalsIgnoreCase(minecraftName))
                return true;
        return false;
    }
}
