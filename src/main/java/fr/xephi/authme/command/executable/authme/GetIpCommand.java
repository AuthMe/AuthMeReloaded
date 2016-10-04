package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

public class GetIpCommand implements ExecutableCommand {

    @Inject
    private BukkitService bukkitService;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        // Get the player query
        String playerName = arguments.get(0);

        Player player = bukkitService.getPlayerExact(playerName);
        if (player == null) {
            sender.sendMessage("The player is not online");
            return;
        }

        sender.sendMessage(player.getName() + "'s IP is: " + player.getAddress().getAddress().getHostAddress()
            + ":" + player.getAddress().getPort());
    }
}
