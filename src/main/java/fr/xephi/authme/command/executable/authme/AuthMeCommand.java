package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.ExecutableCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class AuthMeCommand extends ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        // Show some version info
        // FIXME replace use of commandReference
        sender.sendMessage(ChatColor.GREEN + "This server is running " + AuthMe.getPluginName() + " v" + AuthMe.getPluginVersion() + " b" + AuthMe.getPluginBuildNumber()+ "! " + ChatColor.RED + "<3");
        sender.sendMessage(ChatColor.YELLOW + "Use the command " + ChatColor.GOLD + "/" + commandReference.get(0) + " help" + ChatColor.YELLOW + " to view help.");
        sender.sendMessage(ChatColor.YELLOW + "Use the command " + ChatColor.GOLD + "/" + commandReference.get(0) + " about" + ChatColor.YELLOW + " to view about.");
    }
}
