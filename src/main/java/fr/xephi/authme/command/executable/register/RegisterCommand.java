package fr.xephi.authme.command.executable.register;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.mail.SendMailSSL;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.util.RandomStringUtils;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

import static fr.xephi.authme.settings.properties.EmailSettings.RECOVERY_PASSWORD_LENGTH;
import static fr.xephi.authme.settings.properties.RegistrationSettings.ENABLE_CONFIRM_EMAIL;
import static fr.xephi.authme.settings.properties.RegistrationSettings.USE_EMAIL_REGISTRATION;
import static fr.xephi.authme.settings.properties.RestrictionSettings.ENABLE_PASSWORD_CONFIRMATION;

public class RegisterCommand extends PlayerCommand {

    @Inject
    private Management management;

    @Inject
    private CommandService commandService;

    @Inject
    private SendMailSSL sendMailSsl;

    @Override
    public void runCommand(Player player, List<String> arguments) {
        if (commandService.getProperty(SecuritySettings.PASSWORD_HASH) == HashAlgorithm.TWO_FACTOR) {
            //for two factor auth we don't need to check the usage
            management.performRegister(player, "", "", true);
            return;
        }

        // Ensure that there is 1 argument, or 2 if confirmation is required
        final boolean useConfirmation = isConfirmationRequired();
        if (arguments.isEmpty() || useConfirmation && arguments.size() < 2) {
            commandService.send(player, MessageKey.USAGE_REGISTER);
            return;
        }

        if (commandService.getProperty(USE_EMAIL_REGISTRATION)) {
            handleEmailRegistration(player, arguments);
        } else {
            handlePasswordRegistration(player, arguments);
        }
    }

    @Override
    protected String getAlternativeCommand() {
        return "/authme register <playername> <password>";
    }

    private void handlePasswordRegistration(Player player, List<String> arguments) {
        if (commandService.getProperty(ENABLE_PASSWORD_CONFIRMATION) && !arguments.get(0).equals(arguments.get(1))) {
            commandService.send(player, MessageKey.PASSWORD_MATCH_ERROR);
        } else {
            management.performRegister(player, arguments.get(0), "", true);
        }
    }

    private void handleEmailRegistration(Player player, List<String> arguments) {
        if (!sendMailSsl.hasAllInformation()) {
            commandService.send(player, MessageKey.INCOMPLETE_EMAIL_SETTINGS);
            ConsoleLogger.warning("Cannot register player '" + player.getName() + "': no email or password is set "
                + "to send emails from. Please adjust your config at " + EmailSettings.MAIL_ACCOUNT.getPath());
            return;
        }

        final String email = arguments.get(0);
        if (!commandService.validateEmail(email)) {
            commandService.send(player, MessageKey.INVALID_EMAIL);
        } else if (commandService.getProperty(ENABLE_CONFIRM_EMAIL) && !email.equals(arguments.get(1))) {
            commandService.send(player, MessageKey.USAGE_REGISTER);
        } else {
            String thePass = RandomStringUtils.generate(commandService.getProperty(RECOVERY_PASSWORD_LENGTH));
            management.performRegister(player, thePass, email, true);
        }
    }

    /**
     * Return whether the password or email has to be confirmed.
     *
     * @return True if the confirmation is needed, false otherwise
     */
    private boolean isConfirmationRequired() {
        return commandService.getProperty(USE_EMAIL_REGISTRATION)
            ? commandService.getProperty(ENABLE_CONFIRM_EMAIL)
            : commandService.getProperty(ENABLE_PASSWORD_CONFIRMATION);
    }
}
