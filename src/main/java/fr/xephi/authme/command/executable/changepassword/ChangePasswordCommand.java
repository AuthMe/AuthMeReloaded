package fr.xephi.authme.command.executable.changepassword;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.settings.MessageKey;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.ChangePasswordTask;
import fr.xephi.authme.util.Wrapper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 */
public class ChangePasswordCommand extends ExecutableCommand {

    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // Make sure the current command executor is a player
        if (!(sender instanceof Player)) {
            return true;
        }

        final Wrapper wrapper = Wrapper.getInstance();
        final Messages m = wrapper.getMessages();

        // Get the passwords
        String playerPass = commandArguments.get(0);
        String playerPassVerify = commandArguments.get(1);

        // Get the player instance and make sure it's authenticated
        Player player = (Player) sender;
        String name = player.getName().toLowerCase();
        final PlayerCache playerCache = wrapper.getPlayerCache();
        if (!playerCache.isAuthenticated(name)) {
            m.send(player, MessageKey.NOT_LOGGED_IN);
            return true;
        }

        // Make sure the password is allowed
        // TODO ljacqu 20151121: The password confirmation appears to be never verified
        String playerPassLowerCase = playerPass.toLowerCase();
        if (playerPassLowerCase.contains("delete") || playerPassLowerCase.contains("where")
            || playerPassLowerCase.contains("insert") || playerPassLowerCase.contains("modify")
            || playerPassLowerCase.contains("from") || playerPassLowerCase.contains("select")
            || playerPassLowerCase.contains(";") || playerPassLowerCase.contains("null")
            || !playerPassLowerCase.matches(Settings.getPassRegex)) {
            m.send(player, MessageKey.PASSWORD_MATCH_ERROR);
            return true;
        }
        if (playerPassLowerCase.equalsIgnoreCase(name)) {
            m.send(player, MessageKey.PASSWORD_IS_USERNAME_ERROR);
            return true;
        }
        if (playerPassLowerCase.length() < Settings.getPasswordMinLen
            || playerPassLowerCase.length() > Settings.passwordMaxLength) {
            m.send(player, MessageKey.INVALID_PASSWORD_LENGTH);
            return true;
        }
        if (!Settings.unsafePasswords.isEmpty() && Settings.unsafePasswords.contains(playerPassLowerCase)) {
            m.send(player, MessageKey.PASSWORD_UNSAFE_ERROR);
            return true;
        }

        // Set the password
        final AuthMe plugin = wrapper.getAuthMe();
        wrapper.getServer().getScheduler().runTaskAsynchronously(plugin,
            new ChangePasswordTask(plugin, player, playerPass, playerPassVerify));
        return true;
    }
}
