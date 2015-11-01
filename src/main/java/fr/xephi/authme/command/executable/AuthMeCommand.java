package fr.xephi.authme.command.executable;

import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class AuthMeCommand extends ExecutableCommand {

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
        // Show some version info
        // TODO: Use plugin name and version constants here!
        sender.sendMessage(ChatColor.GREEN + "This server is running AuthMeReloaded v1.0! " + ChatColor.RED + "<3");
        sender.sendMessage(ChatColor.YELLOW + "Use the command " + ChatColor.GOLD + "/" + commandReference.get(0) + " help" + ChatColor.YELLOW + " to view help.");
        sender.sendMessage(ChatColor.YELLOW + "Use the command " + ChatColor.GOLD + "/" + commandReference.get(0) + " about" + ChatColor.YELLOW + " to view about.");
        return true;
    }
}
