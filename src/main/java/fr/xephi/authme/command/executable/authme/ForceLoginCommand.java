package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

import static fr.xephi.authme.permission.PlayerPermission.CAN_LOGIN_BE_FORCED;

/**
 * Forces the login of a player, i.e. logs the player in without the need of a (correct) password.
 */
public class ForceLoginCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        // Get the player query
        String playerName = arguments.isEmpty() ? sender.getName() : arguments.get(0);

        Player player = commandService.getPlayer(playerName);
        if (player == null || !player.isOnline()) {
            sender.sendMessage("Player needs to be online!");
        } else if (!commandService.getPermissionsManager().hasPermission(player, CAN_LOGIN_BE_FORCED)) {
            sender.sendMessage("You cannot force login the player " + playerName + "!");
        } else {
            commandService.getManagement().performLogin(player, "dontneed", true);
            sender.sendMessage("Force login for " + playerName + " performed!");
        }
    }
}
