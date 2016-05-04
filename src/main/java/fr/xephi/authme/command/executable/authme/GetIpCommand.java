package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class GetIpCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        // Get the player query
        String playerName = arguments.get(0);

        Player player = commandService.getPlayer(playerName);
        if (player == null) {
            sender.sendMessage("The player is not online");
            return;
        }

        sender.sendMessage(player.getName() + "'s IP is: " + player.getAddress().getAddress().getHostAddress()
            + ":" + player.getAddress().getPort());
    }
}
