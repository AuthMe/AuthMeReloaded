package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.permission.PlayerPermission;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 */
public class ForceLoginCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        // AuthMe plugin instance
        final AuthMe plugin = AuthMe.getInstance();

        // Get the player query
        String playerName = sender.getName();
        if (arguments.size() >= 1) {
            playerName = arguments.get(0);
        }

        // Command logic
        try {
            // TODO ljacqu 20151212: Retrieve player via Utils method instead
            Player player = Bukkit.getPlayer(playerName);
            if (player == null || !player.isOnline()) {
                sender.sendMessage("Player needs to be online!");
                return;
            }
            if (!plugin.getPermissionsManager().hasPermission(player, PlayerPermission.CAN_LOGIN_BE_FORCED)) {
                sender.sendMessage("You cannot force login for the player " + playerName + "!");
                return;
            }
            plugin.getManagement().performLogin(player, "dontneed", true);
            sender.sendMessage("Force Login for " + playerName + " performed!");
        } catch (Exception e) {
            sender.sendMessage("An error occurred while trying to get that player!");
        }
    }
}
