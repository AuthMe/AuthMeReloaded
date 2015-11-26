package fr.xephi.authme.command.executable.register;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 */
public class RegisterCommand extends ExecutableCommand {

    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {

        // Make sure the sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage("Player Only! Use 'authme register <playername> <password>' instead");
            return true;
        }

        final AuthMe plugin = AuthMe.getInstance();
        final Messages m = plugin.getMessages();

        // Make sure the command arguments are valid
        final Player player = (Player) sender;
        if (commandArguments.getCount() == 0 || (Settings.getEnablePasswordVerifier && commandArguments.getCount() < 2)) {
            m.send(player, "usage_reg");
            return true;
        }

        final Management management = plugin.getManagement();
        if (Settings.emailRegistration && !Settings.getmailAccount.isEmpty()) {
            if (Settings.doubleEmailCheck) {
                if (commandArguments.getCount() < 2 || !commandArguments.get(0).equals(commandArguments.get(1))) {
                    m.send(player, "usage_reg");
                    return true;
                }
            }
            final String email = commandArguments.get(0);
            if (!Settings.isEmailCorrect(email)) {
                m.send(player, "email_invalid");
                return true;
            }
            final String thePass = new RandomString(Settings.getRecoveryPassLength).nextString();
            management.performRegister(player, thePass, email);
            return true;
        }
        if (commandArguments.getCount() > 1 && Settings.getEnablePasswordVerifier) {
            if (!commandArguments.get(0).equals(commandArguments.get(1))) {
                m.send(player, "password_error");
                return true;
            }
        }
        management.performRegister(player, commandArguments.get(0), "");
        return true;
    }
}
