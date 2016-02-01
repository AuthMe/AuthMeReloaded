package fr.xephi.authme.command.executable.changepassword;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.settings.custom.RestrictionSettings;
import fr.xephi.authme.settings.custom.SecuritySettings;
import fr.xephi.authme.task.ChangePasswordTask;
import fr.xephi.authme.util.Wrapper;
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
        Wrapper wrapper = Wrapper.getInstance();
        final PlayerCache playerCache = wrapper.getPlayerCache();
        if (!playerCache.isAuthenticated(name)) {
            commandService.send(player, MessageKey.NOT_LOGGED_IN);
            return;
        }

        // Make sure the password is allowed
        String playerPassLowerCase = newPassword.toLowerCase();
        if (!playerPassLowerCase.matches(commandService.getProperty(RestrictionSettings.ALLOWED_PASSWORD_REGEX))) {
            commandService.send(player, MessageKey.PASSWORD_MATCH_ERROR);
            return;
        }
        if (playerPassLowerCase.equalsIgnoreCase(name)) {
            commandService.send(player, MessageKey.PASSWORD_IS_USERNAME_ERROR);
            return;
        }
        if (playerPassLowerCase.length() < commandService.getProperty(SecuritySettings.MIN_PASSWORD_LENGTH)
            || playerPassLowerCase.length() > commandService.getProperty(SecuritySettings.MAX_PASSWORD_LENGTH)) {
            commandService.send(player, MessageKey.INVALID_PASSWORD_LENGTH);
            return;
        }
        if (commandService.getProperty(SecuritySettings.UNSAFE_PASSWORDS).contains(playerPassLowerCase)) {
            commandService.send(player, MessageKey.PASSWORD_UNSAFE_ERROR);
            return;
        }

        AuthMe plugin = AuthMe.getInstance();
        // TODO ljacqu 20160117: Call async task via Management
        commandService.runTaskAsynchronously(new ChangePasswordTask(plugin, player, oldPassword, newPassword));
    }
}
