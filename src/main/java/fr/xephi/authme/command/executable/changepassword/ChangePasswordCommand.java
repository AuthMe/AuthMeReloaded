package fr.xephi.authme.command.executable.changepassword;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.task.ChangePasswordTask;
import fr.xephi.authme.util.BukkitService;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * The command for a player to change his password with.
 */
public class ChangePasswordCommand extends PlayerCommand {

    @Inject
    private PlayerCache playerCache;

    @Inject
    private BukkitService bukkitService;

    @Inject
    // TODO ljacqu 20160531: Remove this once change password task runs as a process (via Management)
    private PasswordSecurity passwordSecurity;

    @Override
    public void runCommand(Player player, List<String> arguments, CommandService commandService) {
        String oldPassword = arguments.get(0);
        String newPassword = arguments.get(1);

        String name = player.getName().toLowerCase();
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
        bukkitService.runTaskAsynchronously(
            new ChangePasswordTask(plugin, player, oldPassword, newPassword, passwordSecurity));
    }
}
