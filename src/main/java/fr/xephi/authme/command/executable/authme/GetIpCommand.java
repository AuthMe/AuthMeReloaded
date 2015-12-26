package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class GetIpCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        final AuthMe plugin = AuthMe.getInstance();

        // Get the player query
        String playerName = (arguments.size() >= 1) ? arguments.get(0) : sender.getName();

        Player player = Utils.getPlayer(playerName);
        if (player == null) {
            sender.sendMessage("The player is not online");
            return;
        }

        // TODO ljacqu 20151212: Revise the messages (actual IP vs. real IP...?)
        sender.sendMessage(player.getName() + "'s actual IP is : " + player.getAddress().getAddress().getHostAddress()
            + ":" + player.getAddress().getPort());
        sender.sendMessage(player.getName() + "'s real IP is : " + plugin.getIP(player));
    }
}
