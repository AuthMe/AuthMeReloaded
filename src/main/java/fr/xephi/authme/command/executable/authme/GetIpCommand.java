package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.util.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class GetIpCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        // Get the player query
        String playerName = (arguments.size() >= 1) ? arguments.get(0) : sender.getName();

        Player player = Utils.getPlayer(playerName);
        if (player == null) {
            sender.sendMessage("The player is not online");
            return;
        }

        sender.sendMessage(player.getName() + "'s IP is: " + player.getAddress().getAddress().getHostAddress()
            + ":" + player.getAddress().getPort());

        if (commandService.getProperty(HooksSettings.ENABLE_VERYGAMES_IP_CHECK)) {
            sender.sendMessage(player.getName() + "'s real IP is: "
                + commandService.getIpAddressManager().getPlayerIp(player));
        }
    }
}
