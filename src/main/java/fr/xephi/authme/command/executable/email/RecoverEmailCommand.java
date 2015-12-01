package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.Wrapper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.security.NoSuchAlgorithmException;

/**
 */
public class RecoverEmailCommand extends ExecutableCommand {

    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // Make sure the current command executor is a player
        if (!(sender instanceof Player)) {
            return true;
        }

        // Get the parameter values
        String playerMail = commandArguments.get(0);

        // Get the player instance and name
        final Player player = (Player) sender;
        final String playerName = player.getName();

        // Command logic
        final Wrapper wrapper = Wrapper.getInstance();
        final AuthMe plugin = wrapper.getAuthMe();
        final Messages m = wrapper.getMessages();

        if (plugin.mail == null) {
            m.send(player, MessageKey.ERROR);
            return true;
        }
        if (plugin.database.isAuthAvailable(playerName)) {
            if (PlayerCache.getInstance().isAuthenticated(playerName)) {
                m.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
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
                    m.send(player, MessageKey.UNKNOWN_USER);
                    return true;
                }
                if (Settings.getmailAccount.equals("") || Settings.getmailAccount.isEmpty()) {
                    m.send(player, MessageKey.ERROR);
                    return true;
                }

                if (!playerMail.equalsIgnoreCase(auth.getEmail()) || playerMail.equalsIgnoreCase("your@email.com") || auth.getEmail().equalsIgnoreCase("your@email.com")) {
                    m.send(player, MessageKey.INVALID_EMAIL);
                    return true;
                }
                auth.setHash(hashNew);
                plugin.database.updatePassword(auth);
                plugin.mail.main(auth, thePass);
                m.send(player, MessageKey.RECOVERY_EMAIL_SENT_MESSAGE);
            } catch (NoSuchAlgorithmException | NoClassDefFoundError ex) {
                ex.printStackTrace();
                ConsoleLogger.showError(ex.getMessage());
                m.send(sender, MessageKey.ERROR);
            }
        } else {
            m.send(player, MessageKey.REGISTER_EMAIL_MESSAGE);
        }

        return true;
    }
}
