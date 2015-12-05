package fr.xephi.authme.command.executable.register;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.Wrapper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

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

        List<String> arguments = commandArguments.getList();

        final Wrapper wrapper = Wrapper.getInstance();
        final AuthMe plugin = wrapper.getAuthMe();
        final Messages m = wrapper.getMessages();

        // Make sure the command arguments are valid
        final Player player = (Player) sender;
        if (arguments.isEmpty() || (Settings.getEnablePasswordVerifier && arguments.size() < 2)) {
            m.send(player, MessageKey.USAGE_REGISTER);
            return true;
        }

        final Management management = plugin.getManagement();
        if (Settings.emailRegistration && !Settings.getmailAccount.isEmpty()) {
            if (Settings.doubleEmailCheck && arguments.size() < 2 || !arguments.get(0).equals(arguments.get(1))) {
                m.send(player, MessageKey.USAGE_REGISTER);
                return true;
            }
            final String email = arguments.get(0);
            if (!Settings.isEmailCorrect(email)) {
                m.send(player, MessageKey.INVALID_EMAIL);
                return true;
            }
            final String thePass = new RandomString(Settings.getRecoveryPassLength).nextString();
            management.performRegister(player, thePass, email);
            return true;
        }
        if (arguments.size() > 1 && Settings.getEnablePasswordVerifier) {
            if (!arguments.get(0).equals(commandArguments.get(1))) {
                m.send(player, MessageKey.PASSWORD_MATCH_ERROR);
                return true;
            }
        }
        management.performRegister(player, commandArguments.get(0), "");
        return true;
    }
}
