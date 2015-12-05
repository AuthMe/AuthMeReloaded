package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 */
public class GetIpCommand extends ExecutableCommand {

    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        final AuthMe plugin = AuthMe.getInstance();
        List<String> arguments = commandArguments.getList();

        // Get the player query
        String playerName = (arguments.size() >= 1) ? arguments.get(0) : sender.getName();

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
