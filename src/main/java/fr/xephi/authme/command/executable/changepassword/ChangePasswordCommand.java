package fr.xephi.authme.command.executable.changepassword;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.ChangePasswordTask;
import fr.xephi.authme.util.Wrapper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * The command for a player to change his password with.
 */
public class ChangePasswordCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        // Make sure the current command executor is a player
        if (!(sender instanceof Player)) {
            return;
        }

        final Wrapper wrapper = Wrapper.getInstance();
        final Messages m = wrapper.getMessages();

        // Get the passwords
        String oldPassword = arguments.get(0);
        String newPassword = arguments.get(1);

        // Get the player instance and make sure he's authenticated
        Player player = (Player) sender;
        String name = player.getName().toLowerCase();
        final PlayerCache playerCache = wrapper.getPlayerCache();
        if (!playerCache.isAuthenticated(name)) {
            m.send(player, MessageKey.NOT_LOGGED_IN);
            return;
        }

        // Make sure the password is allowed
        String playerPassLowerCase = newPassword.toLowerCase();
        if (playerPassLowerCase.contains("delete") || playerPassLowerCase.contains("where")
            || playerPassLowerCase.contains("insert") || playerPassLowerCase.contains("modify")
            || playerPassLowerCase.contains("from") || playerPassLowerCase.contains("select")
            || playerPassLowerCase.contains(";") || playerPassLowerCase.contains("null")
            || !playerPassLowerCase.matches(Settings.getPassRegex)) {
            m.send(player, MessageKey.PASSWORD_MATCH_ERROR);
            return;
        }
        if (playerPassLowerCase.equalsIgnoreCase(name)) {
            m.send(player, MessageKey.PASSWORD_IS_USERNAME_ERROR);
            return;
        }
        if (playerPassLowerCase.length() < Settings.getPasswordMinLen
            || playerPassLowerCase.length() > Settings.passwordMaxLength) {
            m.send(player, MessageKey.INVALID_PASSWORD_LENGTH);
            return;
        }
        if (!Settings.unsafePasswords.isEmpty() && Settings.unsafePasswords.contains(playerPassLowerCase)) {
            m.send(player, MessageKey.PASSWORD_UNSAFE_ERROR);
            return;
        }

        // Set the password
        final AuthMe plugin = wrapper.getAuthMe();
        wrapper.getScheduler().runTaskAsynchronously(plugin,
            new ChangePasswordTask(plugin, player, oldPassword, newPassword));
    }
}
