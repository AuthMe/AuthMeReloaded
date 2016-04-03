package fr.xephi.authme.command.executable.register;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.entity.Player;

import java.util.List;

import static fr.xephi.authme.settings.properties.EmailSettings.RECOVERY_PASSWORD_LENGTH;
import static fr.xephi.authme.settings.properties.RestrictionSettings.ENABLE_PASSWORD_CONFIRMATION;

public class RegisterCommand extends PlayerCommand {

    @Override
    public void runCommand(Player player, List<String> arguments, CommandService commandService) {
        if (commandService.getProperty(SecuritySettings.PASSWORD_HASH) == HashAlgorithm.TWO_FACTOR) {
            //for two factor auth we don't need to check the usage
            commandService.getManagement().performRegister(player, "", "");
            return;
        }

        if (arguments.isEmpty() || commandService.getProperty(ENABLE_PASSWORD_CONFIRMATION) && arguments.size() < 2) {
            commandService.send(player, MessageKey.USAGE_REGISTER);
            return;
        }

        final Management management = commandService.getManagement();
        if (commandService.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)
                && !commandService.getProperty(EmailSettings.MAIL_ACCOUNT).isEmpty()) {
            boolean emailDoubleCheck = commandService.getProperty(RegistrationSettings.ENABLE_CONFIRM_EMAIL);
            if (emailDoubleCheck && arguments.size() < 2 || !arguments.get(0).equals(arguments.get(1))) {
                commandService.send(player, MessageKey.USAGE_REGISTER);
                return;
            }

            final String email = arguments.get(0);
            if (!commandService.validateEmail(email)) {
                commandService.send(player, MessageKey.INVALID_EMAIL);
                return;
            }

            final String thePass = RandomString.generate(commandService.getProperty(RECOVERY_PASSWORD_LENGTH));
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
