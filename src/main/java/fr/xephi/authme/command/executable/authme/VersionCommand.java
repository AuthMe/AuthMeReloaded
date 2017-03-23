package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

public class VersionCommand implements ExecutableCommand {

    @Inject
    private BukkitService bukkitService;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        // Show some version info
        sender.sendMessage(ChatColor.GOLD + "==========[ " + AuthMe.getPluginName() + " ABOUT ]==========");
        sender.sendMessage(ChatColor.GOLD + "Version: " + ChatColor.WHITE + AuthMe.getPluginName()
            + " v" + AuthMe.getPluginVersion() + ChatColor.GRAY + " (build: " + AuthMe.getPluginBuildNumber() + ")");
        sender.sendMessage(ChatColor.GOLD + "Developers:");
        Collection<? extends Player> onlinePlayers = bukkitService.getOnlinePlayers();
        printDeveloper(sender, "Alexandre Vanhecke", "xephi59", "Original Author", onlinePlayers);
        printDeveloper(sender, "Lucas J.", "ljacqu", "Main Developer", onlinePlayers);
        printDeveloper(sender, "Gnat008", "gnat008", "Developer", onlinePlayers);
        printDeveloper(sender, "DNx5", "DNx5", "Developer", onlinePlayers);
        printDeveloper(sender, "games647", "games647", "Developer", onlinePlayers);
        printDeveloper(sender, "Tim Visee", "timvisee", "Developer", onlinePlayers);
        printDeveloper(sender, "Gabriele C.", "sgdc3", "Project manager, Contributor", onlinePlayers);
        sender.sendMessage(ChatColor.GOLD + "Website: " + ChatColor.WHITE
            + "http://dev.bukkit.org/bukkit-plugins/authme-reloaded/");
        sender.sendMessage(ChatColor.GOLD + "License: " + ChatColor.WHITE + "GNU GPL v3.0"
            + ChatColor.GRAY + ChatColor.ITALIC + " (See LICENSE file)");
        sender.sendMessage(ChatColor.GOLD + "Copyright: " + ChatColor.WHITE
            + "Copyright (c) AuthMe-Team 2017. All rights reserved.");
    }

    /**
     * Print a developer with proper styling.
     *
     * @param sender        The command sender
     * @param name          The display name of the developer
     * @param minecraftName The Minecraft username of the developer, if available
     * @param function      The function of the developer
     * @param onlinePlayers The list of online players
     */
    private static void printDeveloper(CommandSender sender, String name, String minecraftName, String function,
                                       Collection<? extends Player> onlinePlayers) {
        // Print the name
        StringBuilder msg = new StringBuilder();
        msg.append(" ")
            .append(ChatColor.WHITE)
            .append(name);

        // Append the Minecraft name
        msg.append(ChatColor.GRAY).append(" // ").append(ChatColor.WHITE).append(minecraftName);
        msg.append(ChatColor.GRAY).append(ChatColor.ITALIC).append(" (").append(function).append(")");

        // Show the online status
        if (isPlayerOnline(minecraftName, onlinePlayers)) {
            msg.append(ChatColor.GREEN).append(ChatColor.ITALIC).append(" (In-Game)");
        }

        // Print the message
        sender.sendMessage(msg.toString());
    }

    /**
     * Check whether a player is online.
     *
     * @param minecraftName The Minecraft player name
     * @param onlinePlayers List of online players
     *
     * @return True if the player is online, false otherwise
     */
    private static boolean isPlayerOnline(String minecraftName, Collection<? extends Player> onlinePlayers) {
        for (Player player : onlinePlayers) {
            if (player.getName().equalsIgnoreCase(minecraftName)) {
                return true;
            }
        }
        return false;
    }
}
