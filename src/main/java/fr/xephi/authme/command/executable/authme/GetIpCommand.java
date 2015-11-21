package fr.xephi.authme.command.executable.authme;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;

/**
 */
public class GetIpCommand extends ExecutableCommand {

    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // AuthMe plugin instance
        final AuthMe plugin = AuthMe.getInstance();

        // Get the player query
        String playerName = sender.getName();
        if(commandArguments.getCount() >= 1)
            playerName = commandArguments.get(0);

        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sender.sendMessage("This player is not actually online");
            return true;
        }
        sender.sendMessage(player.getName() + "'s actual IP is : " + player.getAddress().getAddress().getHostAddress() + ":" + player.getAddress().getPort());
        sender.sendMessage(player.getName() + "'s real IP is : " + plugin.getIP(player));
        return true;
    }
}
