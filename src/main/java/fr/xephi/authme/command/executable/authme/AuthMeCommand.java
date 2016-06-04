package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.ExecutableCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * AuthMe base command; shows the version and some command pointers.
 */
public class AuthMeCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        sender.sendMessage(ChatColor.GREEN + "This server is running " + AuthMe.getPluginName() + " v"
            + AuthMe.getPluginVersion() + " b" + AuthMe.getPluginBuildNumber()+ "! " + ChatColor.RED + "<3");
        sender.sendMessage(ChatColor.YELLOW + "Use the command " + ChatColor.GOLD + "/authme help" + ChatColor.YELLOW
            + " to view help.");
        sender.sendMessage(ChatColor.YELLOW + "Use the command " + ChatColor.GOLD + "/authme about" + ChatColor.YELLOW
            + " to view about.");
    }
}
