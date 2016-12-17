package fr.xephi.authme.command.executable.register;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.mail.SendMailSSL;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.RegistrationArgumentType;
import fr.xephi.authme.settings.properties.RegistrationArgumentType.Execution;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.RandomStringUtils;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

import static fr.xephi.authme.settings.properties.EmailSettings.RECOVERY_PASSWORD_LENGTH;
import static fr.xephi.authme.settings.properties.RegistrationArgumentType.PASSWORD_WITH_CONFIRMATION;
import static fr.xephi.authme.settings.properties.RegistrationArgumentType.PASSWORD_WITH_EMAIL;
import static fr.xephi.authme.settings.properties.RegistrationSettings.REGISTRATION_TYPE;

/**
 * Command for /register.
 */
public class RegisterCommand extends PlayerCommand {

    @Inject
    private Management management;

    @Inject
    private CommonService commonService;

    @Inject
    private SendMailSSL sendMailSsl;

    @Inject
    private ValidationService validationService;

    @Override
    public void runCommand(Player player, List<String> arguments) {
        if (commonService.getProperty(SecuritySettings.PASSWORD_HASH) == HashAlgorithm.TWO_FACTOR) {
            //for two factor auth we don't need to check the usage
            management.performRegister(player, "", "", true);
            return;
        }

        // Ensure that there is 1 argument, or 2 if confirmation is required
        RegistrationArgumentType registerType = commonService.getProperty(REGISTRATION_TYPE);
        if (registerType.getRequiredNumberOfArgs() > arguments.size()) {
            commonService.send(player, MessageKey.USAGE_REGISTER);
            return;
        }

        if (registerType.getExecution() == Execution.EMAIL) {
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
        RegistrationArgumentType registrationType = commonService.getProperty(REGISTRATION_TYPE);
        if (registrationType == PASSWORD_WITH_CONFIRMATION && !arguments.get(0).equals(arguments.get(1))) {
            commonService.send(player, MessageKey.PASSWORD_MATCH_ERROR);
        } else if (registrationType == PASSWORD_WITH_EMAIL && !validationService.validateEmail(arguments.get(1))) {
            commonService.send(player, MessageKey.INVALID_EMAIL);
        } else {
            // We might only have received one argument, need to check that it's safe to do arguments.get(1)
            String email = registrationType == PASSWORD_WITH_EMAIL ? arguments.get(1) : null;
            management.performRegister(player, arguments.get(0), email, true);
        }
    }

    private void handleEmailRegistration(Player player, List<String> arguments) {
        if (!sendMailSsl.hasAllInformation()) {
            commonService.send(player, MessageKey.INCOMPLETE_EMAIL_SETTINGS);
            ConsoleLogger.warning("Cannot register player '" + player.getName() + "': no email or password is set "
                + "to send emails from. Please adjust your config at " + EmailSettings.MAIL_ACCOUNT.getPath());
            return;
        }

        final String email = arguments.get(0);
        if (!validationService.validateEmail(email)) {
            commonService.send(player, MessageKey.INVALID_EMAIL);
        } else if (commonService.getProperty(REGISTRATION_TYPE) == RegistrationArgumentType.EMAIL_WITH_CONFIRMATION
                && !email.equals(arguments.get(1))) {
            commonService.send(player, MessageKey.USAGE_REGISTER);
        } else {
            String thePass = RandomStringUtils.generate(commonService.getProperty(RECOVERY_PASSWORD_LENGTH));
            management.performRegister(player, thePass, email, true);
        }
    }
}
