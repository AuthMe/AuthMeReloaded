package fr.xephi.authme.command.executable.changepassword;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.task.ChangePasswordTask;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * The command for a player to change his password with.
 */
public class ChangePasswordCommand extends PlayerCommand {

    @Override
    public void runCommand(Player player, List<String> arguments, CommandService commandService) {
        String oldPassword = arguments.get(0);
        String newPassword = arguments.get(1);

        String name = player.getName().toLowerCase();
        final PlayerCache playerCache = commandService.getPlayerCache();
        if (!playerCache.isAuthenticated(name)) {
            commandService.send(player, MessageKey.NOT_LOGGED_IN);
            return;
        }

        // Make sure the password is allowed
        MessageKey passwordError = commandService.validatePassword(newPassword, name);
        if (passwordError != null) {
            commandService.send(player, passwordError);
            return;
        }

        AuthMe plugin = AuthMe.getInstance();
        // TODO ljacqu 20160117: Call async task via Management
        commandService.runTaskAsynchronously(new ChangePasswordTask(plugin, player, oldPassword, newPassword));
    }
}
