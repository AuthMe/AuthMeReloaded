package fr.xephi.authme.command.executable.changepassword;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.ChangePasswordTask;

/**
 */
public class ChangePasswordCommand extends ExecutableCommand {

    /**
     * Execute the command.
     *
     * @param sender The command sender.
     * @param commandReference The command reference.
     * @param commandArguments The command arguments.
     *
    
     * @return True if the command was executed successfully, false otherwise. */
    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // AuthMe plugin instance
        final AuthMe plugin = AuthMe.getInstance();

        // Messages instance
        final Messages m = Messages.getInstance();

        // Get the passwords
        String playerPass = commandArguments.get(0);
        String playerPassVerify = commandArguments.get(1);

        // Make sure the current command executor is a player
        if(!(sender instanceof Player)) {
            return true;
        }

        // Get the player instance and make sure it's authenticated
        Player player = (Player) sender;
        String name = player.getName().toLowerCase();
        if (!PlayerCache.getInstance().isAuthenticated(name)) {
            m.send(player, "not_logged_in");
            return true;
        }

        // Make sure the password is allowed
        String playerPassLowerCase = playerPass.toLowerCase();
        if (playerPassLowerCase.contains("delete") || playerPassLowerCase.contains("where") || playerPassLowerCase.contains("insert") || playerPassLowerCase.contains("modify") || playerPassLowerCase.contains("from") || playerPassLowerCase.contains("select") || playerPassLowerCase.contains(";") || playerPassLowerCase.contains("null") || !playerPassLowerCase.matches(Settings.getPassRegex)) {
            m.send(player, "password_error");
            return true;
        }
        if (playerPassLowerCase.equalsIgnoreCase(name)) {
            m.send(player, "password_error_nick");
            return true;
        }
        if (playerPassLowerCase.length() < Settings.getPasswordMinLen || playerPassLowerCase.length() > Settings.passwordMaxLength) {
            m.send(player, "pass_len");
            return true;
        }
        if (!Settings.unsafePasswords.isEmpty()) {
            if (Settings.unsafePasswords.contains(playerPassLowerCase)) {
                m.send(player, "password_error_unsafe");
                return true;
            }
        }

        // Set the password
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new ChangePasswordTask(plugin, player, playerPass, playerPassVerify));
        return true;
    }
}
