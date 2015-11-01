package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.security.NoSuchAlgorithmException;

public class RecoverEmailCommand extends ExecutableCommand {

    /**
     * Execute the command.
     *
     * @param sender The command sender.
     * @param commandReference The command reference.
     * @param commandArguments The command arguments.
     *
     * @return True if the command was executed successfully, false otherwise.
     */
    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // AuthMe plugin instance
        final AuthMe plugin = AuthMe.getInstance();

        // Messages instance
        final Messages m = Messages.getInstance();

        // Get the parameter values
        String playerMail = commandArguments.get(0);

        // Make sure the current command executor is a player
        if(!(sender instanceof Player)) {
            return true;
        }

        // Get the player instance and name
        final Player player = (Player) sender;
        final String playerName = player.getName();

        // Command logic
        if (plugin.mail == null) {
            m.send(player, "error");
            return true;
        }
        if (plugin.database.isAuthAvailable(playerName)) {
            if (PlayerCache.getInstance().isAuthenticated(playerName)) {
                m.send(player, "logged_in");
                return true;
            }
            try {
                RandomString rand = new RandomString(Settings.getRecoveryPassLength);
                String thePass = rand.nextString();
                String hashNew = PasswordSecurity.getHash(Settings.getPasswordHash, thePass, playerName);
                PlayerAuth auth;
                if (PlayerCache.getInstance().isAuthenticated(playerName)) {
                    auth = PlayerCache.getInstance().getAuth(playerName);
                } else if (plugin.database.isAuthAvailable(playerName)) {
                    auth = plugin.database.getAuth(playerName);
                } else {
                    m.send(player, "unknown_user");
                    return true;
                }
                if (Settings.getmailAccount.equals("") || Settings.getmailAccount.isEmpty()) {
                    m.send(player, "error");
                    return true;
                }

                if (!playerMail.equalsIgnoreCase(auth.getEmail()) || playerMail.equalsIgnoreCase("your@email.com") || auth.getEmail().equalsIgnoreCase("your@email.com")) {
                    m.send(player, "email_invalid");
                    return true;
                }
                auth.setHash(hashNew);
                plugin.database.updatePassword(auth);
                plugin.mail.main(auth, thePass);
                m.send(player, "email_send");
            } catch (NoSuchAlgorithmException | NoClassDefFoundError ex) {
                ConsoleLogger.showError(ex.getMessage());
                m.send(sender, "error");
            }
        } else {
            m.send(player, "reg_email_msg");
        }

        return true;
    }
}
