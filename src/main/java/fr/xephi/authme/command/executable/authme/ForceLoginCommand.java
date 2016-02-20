package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.util.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Forces the login of a player, i.e. logs the player in without the need of a (correct) password.
 */
public class ForceLoginCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        // Get the player query
        String playerName = arguments.isEmpty() ? sender.getName() : arguments.get(0);

        Player player = Utils.getPlayer(playerName);
        if (player == null || !player.isOnline()) {
            sender.sendMessage("Player needs to be online!");
        } else if (!commandService.getPermissionsManager()
            .hasPermission(player, PlayerPermission.CAN_LOGIN_BE_FORCED)) {
            sender.sendMessage("You cannot force login for the player " + playerName + "!");
        } else {
            commandService.getManagement().performLogin(player, "dontneed", true);
            sender.sendMessage("Force Login for " + playerName + " performed!");
        }
    }
}
