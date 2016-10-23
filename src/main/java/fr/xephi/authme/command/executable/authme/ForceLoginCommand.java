package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

import static fr.xephi.authme.permission.PlayerPermission.CAN_LOGIN_BE_FORCED;

/**
 * Forces the login of a player, i.e. logs the player in without the need of a (correct) password.
 */
public class ForceLoginCommand implements ExecutableCommand {

    @Inject
    private PermissionsManager permissionsManager;

    @Inject
    private Management management;

    @Inject
    private BukkitService bukkitService;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        // Get the player query
        String playerName = arguments.isEmpty() ? sender.getName() : arguments.get(0);

        Player player = bukkitService.getPlayerExact(playerName);
        if (player == null || !player.isOnline()) {
            sender.sendMessage("Player needs to be online!");
        } else if (!permissionsManager.hasPermission(player, CAN_LOGIN_BE_FORCED)) {
            sender.sendMessage("You cannot force login the player " + playerName + "!");
        } else {
            management.forceLogin(player);
            sender.sendMessage("Force login for " + playerName + " performed!");
        }
    }
}
