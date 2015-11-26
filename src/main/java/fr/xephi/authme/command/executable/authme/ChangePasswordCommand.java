package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.security.NoSuchAlgorithmException;

/**
 */
public class ChangePasswordCommand extends ExecutableCommand {

    @Override
    public boolean executeCommand(final CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        final AuthMe plugin = AuthMe.getInstance();
        // Messages instance
        final Messages m = plugin.getMessages();

        // Get the player and password
        String playerName = commandArguments.get(0);
        final String playerPass = commandArguments.get(1);

        // Validate the password
        String playerPassLowerCase = playerPass.toLowerCase();
        if (playerPassLowerCase.contains("delete") || playerPassLowerCase.contains("where") || playerPassLowerCase.contains("insert") || playerPassLowerCase.contains("modify") || playerPassLowerCase.contains("from") || playerPassLowerCase.contains("select") || playerPassLowerCase.contains(";") || playerPassLowerCase.contains("null") || !playerPassLowerCase.matches(Settings.getPassRegex)) {
            m.send(sender, "password_error");
            return true;
        }
        if (playerPassLowerCase.equalsIgnoreCase(playerName)) {
            m.send(sender, "password_error_nick");
            return true;
        }
        if (playerPassLowerCase.length() < Settings.getPasswordMinLen || playerPassLowerCase.length() > Settings.passwordMaxLength) {
            m.send(sender, "pass_len");
            return true;
        }
        if (!Settings.unsafePasswords.isEmpty()) {
            if (Settings.unsafePasswords.contains(playerPassLowerCase)) {
                m.send(sender, "password_error_unsafe");
                return true;
            }
        }
        // Set the password
        final String playerNameLowerCase = playerName.toLowerCase();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                String hash;
                try {
                    hash = PasswordSecurity.getHash(Settings.getPasswordHash, playerPass, playerNameLowerCase);
                } catch (NoSuchAlgorithmException e) {
                    m.send(sender, "error");
                    return;
                }
                PlayerAuth auth = null;
                if (PlayerCache.getInstance().isAuthenticated(playerNameLowerCase)) {
                    auth = PlayerCache.getInstance().getAuth(playerNameLowerCase);
                } else if (plugin.database.isAuthAvailable(playerNameLowerCase)) {
                    auth = plugin.database.getAuth(playerNameLowerCase);
                }
                if (auth == null) {
                    m.send(sender, "unknown_user");
                    return;
                }
                auth.setHash(hash);
                if (PasswordSecurity.userSalt.containsKey(playerNameLowerCase)) {
                    auth.setSalt(PasswordSecurity.userSalt.get(playerNameLowerCase));
                    plugin.database.updateSalt(auth);
                }
                if (!plugin.database.updatePassword(auth)) {
                    m.send(sender, "error");
                    return;
                }
                sender.sendMessage("pwd_changed");
                ConsoleLogger.info(playerNameLowerCase + "'s password changed");
            }

        });
        return true;
    }
}
