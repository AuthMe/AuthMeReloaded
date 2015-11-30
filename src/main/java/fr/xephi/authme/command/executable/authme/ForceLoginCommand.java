package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.permission.UserPermission;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 */
public class ForceLoginCommand extends ExecutableCommand {

    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // AuthMe plugin instance
        final AuthMe plugin = AuthMe.getInstance();

        // Get the player query
        String playerName = sender.getName();
        if (commandArguments.getCount() >= 1)
            playerName = commandArguments.get(0);

        // Command logic
        try {
            @SuppressWarnings("deprecation")
            Player player = Bukkit.getPlayer(playerName);
            if (player == null || !player.isOnline()) {
                sender.sendMessage("Player needs to be online!");
                return true;
            }
            if (!plugin.getPermissionsManager().hasPermission(player, UserPermission.CAN_LOGIN_BE_FORCED)) {
                sender.sendMessage("You cannot force login for the player " + playerName + "!");
                return true;
            }
            plugin.management.performLogin(player, "dontneed", true);
            sender.sendMessage("Force Login for " + playerName + " performed!");
        } catch (Exception e) {
            sender.sendMessage("An error occurred while trying to get that player!");
        }

        return true;
    }
}
