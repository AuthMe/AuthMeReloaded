package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 */
public class AuthMeCommand extends ExecutableCommand {

    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // Show some version info
        sender.sendMessage(ChatColor.GREEN + "This server is running " + AuthMe.getPluginName() + " v" + AuthMe.getVersionName() + "! " + ChatColor.RED + "<3");
        sender.sendMessage(ChatColor.YELLOW + "Use the command " + ChatColor.GOLD + "/" + commandReference.get(0) + " help" + ChatColor.YELLOW + " to view help.");
        sender.sendMessage(ChatColor.YELLOW + "Use the command " + ChatColor.GOLD + "/" + commandReference.get(0) + " about" + ChatColor.YELLOW + " to view about.");
        return true;
    }
}
