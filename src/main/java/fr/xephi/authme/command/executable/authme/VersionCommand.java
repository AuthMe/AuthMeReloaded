package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import static fr.xephi.authme.settings.properties.PluginSettings.HELP_HEADER;
import fr.xephi.authme.util.Utils;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VersionCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        // Show some version info
        sender.sendMessage(ChatColor.GOLD + "==========[ " + commandService.getProperty(HELP_HEADER)
            + " ABOUT ]==========");
        sender.sendMessage(ChatColor.GOLD + "Version: " + ChatColor.WHITE + AuthMe.getPluginName()
            + " v" + AuthMe.getPluginVersion() + ChatColor.GRAY + " (build: " + AuthMe.getPluginBuildNumber() + ")");
        sender.sendMessage(ChatColor.GOLD + "Developers:");
        printDeveloper(sender, "Xephi", "xephi59", "Lead Developer");
        printDeveloper(sender, "DNx5", "DNx5", "Developer");
        printDeveloper(sender, "games647", "games647", "Developer");
        printDeveloper(sender, "Tim Visee", "timvisee", "Developer");
        printDeveloper(sender, "Sgdc3", "sgdc3", "Project manager, Contributor");
        sender.sendMessage(ChatColor.GOLD + "Website: " + ChatColor.WHITE +
            "http://dev.bukkit.org/bukkit-plugins/authme-reloaded/");
        sender.sendMessage(ChatColor.GOLD + "License: " + ChatColor.WHITE + "GNU GPL v3.0"
            + ChatColor.GRAY + ChatColor.ITALIC + " (See LICENSE file)");
        sender.sendMessage(ChatColor.GOLD + "Copyright: " + ChatColor.WHITE
            + "Copyright (c) Xephi 2015. All rights reserved.");
    }

    /**
     * Print a developer with proper styling.
     *
     * @param sender        The command sender.
     * @param name          The display name of the developer.
     * @param minecraftName The Minecraft username of the developer, if available.
     * @param function      The function of the developer.
     */
    private static void printDeveloper(CommandSender sender, String name, String minecraftName, String function) {
        // Print the name
        StringBuilder msg = new StringBuilder();
        msg.append(" ")
            .append(ChatColor.WHITE)
            .append(name);

        // Append the Minecraft name
        msg.append(ChatColor.GRAY).append(" // ").append(ChatColor.WHITE).append(minecraftName);
        msg.append(ChatColor.GRAY).append(ChatColor.ITALIC).append(" (").append(function).append(")");

        // Show the online status
        if (isPlayerOnline(minecraftName)) {
            msg.append(ChatColor.GREEN).append(ChatColor.ITALIC).append(" (In-Game)");
        }

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
    private static boolean isPlayerOnline(String minecraftName) {
        // Note ljacqu 20151121: Generally you should use Utils#getOnlinePlayers to retrieve the list of online players.
        // If it's only used in a for-each loop such as here, it's fine. For other purposes, go through the Utils class.
        for (Player player : Utils.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(minecraftName)) {
                return true;
            }
        }
        return false;
    }
}
