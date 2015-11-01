package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.security.NoSuchAlgorithmException;

public class RegisterCommand extends ExecutableCommand {

    /**
     * Execute the command.
     *
     * @param sender           The command sender.
     * @param commandReference The command reference.
     * @param commandArguments The command arguments.
     *
     * @return True if the command was executed successfully, false otherwise.
     */
    @Override
    public boolean executeCommand(final CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // AuthMe plugin instance
        final AuthMe plugin = AuthMe.getInstance();

        // Messages instance
        final Messages m = Messages.getInstance();

        // Get the player name and password
        final String playerName = commandArguments.get(0);
        final String playerPass = commandArguments.get(1);
        final String playerNameLowerCase = playerName.toLowerCase();
        final String playerPassLowerCase = playerPass.toLowerCase();

        // Command logic
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
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @SuppressWarnings("deprecation")
            @Override
            public void run() {
                try {
                    if (plugin.database.isAuthAvailable(playerNameLowerCase)) {
                        m.send(sender, "user_regged");
                        return;
                    }
                    String hash = PasswordSecurity.getHash(Settings.getPasswordHash, playerPassLowerCase, playerNameLowerCase);
                    PlayerAuth auth = new PlayerAuth(playerNameLowerCase, hash, "192.168.0.1", 0L, "your@email.com", playerName);
                    if (PasswordSecurity.userSalt.containsKey(playerNameLowerCase) && PasswordSecurity.userSalt.get(playerNameLowerCase) != null)
                        auth.setSalt(PasswordSecurity.userSalt.get(playerNameLowerCase));
                    else auth.setSalt("");
                    if (!plugin.database.saveAuth(auth)) {
                        m.send(sender, "error");
                        return;
                    }
                    plugin.database.setUnlogged(playerNameLowerCase);
                    if (Bukkit.getPlayerExact(playerName) != null)
                        Bukkit.getPlayerExact(playerName).kickPlayer("An admin just registered you, please log again");
                    m.send(sender, "registered");
                    ConsoleLogger.info(playerNameLowerCase + " registered");
                } catch (NoSuchAlgorithmException ex) {
                    ConsoleLogger.showError(ex.getMessage());
                    m.send(sender, "error");
                }

            }
        });
        return true;
    }
}
