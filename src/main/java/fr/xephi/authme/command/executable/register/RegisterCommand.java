package fr.xephi.authme.command.executable.register;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;

/**
 */
public class RegisterCommand extends ExecutableCommand {

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

        // Make sure the sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage("Player Only! Use 'authme register <playername> <password>' instead");
            return true;
        }

        // Make sure the command arguments are valid
        final Player player = (Player) sender;
        if (commandArguments.getCount() == 0 || (Settings.getEnablePasswordVerifier && commandArguments.getCount() < 2)) {
            m.send(player, "usage_reg");
            return true;
        }

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
            RandomString rand = new RandomString(Settings.getRecoveryPassLength);
            final String thePass = rand.nextString();
            plugin.management.performRegister(player, thePass, email);
            return true;
        }
        if (commandArguments.getCount() > 1 && Settings.getEnablePasswordVerifier)
            if (!commandArguments.get(0).equals(commandArguments.get(1))) {
                m.send(player, "password_error");
                return true;
            }
        plugin.management.performRegister(player, commandArguments.get(0), "");
        return true;
    }
}
