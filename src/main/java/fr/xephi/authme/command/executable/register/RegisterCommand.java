package fr.xephi.authme.command.executable.register;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.Settings;
import org.bukkit.entity.Player;

import java.util.List;

public class RegisterCommand extends PlayerCommand {

    @Override
    public void runCommand(Player player, List<String> arguments, CommandService commandService) {
        if (arguments.isEmpty() || Settings.enablePasswordConfirmation && arguments.size() < 2) {
            commandService.send(player, MessageKey.USAGE_REGISTER);
            return;
        }

        final Management management = commandService.getManagement();
        if (Settings.emailRegistration && !Settings.getmailAccount.isEmpty()) {
            if (Settings.doubleEmailCheck && arguments.size() < 2 || !arguments.get(0).equals(arguments.get(1))) {
                commandService.send(player, MessageKey.USAGE_REGISTER);
                return;
            }
            final String email = arguments.get(0);
            if (!Settings.isEmailCorrect(email)) {
                commandService.send(player, MessageKey.INVALID_EMAIL);
                return;
            }
            final String thePass = RandomString.generate(Settings.getRecoveryPassLength);
            management.performRegister(player, thePass, email);
            return;
        }
        if (arguments.size() > 1 && Settings.enablePasswordConfirmation && !arguments.get(0).equals(arguments.get(1))) {
            commandService.send(player, MessageKey.PASSWORD_MATCH_ERROR);
            return;
        }
        management.performRegister(player, arguments.get(0), "");
    }

    @Override
    public String getAlternativeCommand() {
        return "/authme register <playername> <password>";
    }
}
