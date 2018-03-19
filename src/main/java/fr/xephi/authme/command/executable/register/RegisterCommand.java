package fr.xephi.authme.command.executable.register;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.data.captcha.RegistrationCaptchaManager;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import fr.xephi.authme.process.register.executors.EmailRegisterParams;
import fr.xephi.authme.process.register.executors.PasswordRegisterParams;
import fr.xephi.authme.process.register.executors.RegistrationMethod;
import fr.xephi.authme.process.register.executors.TwoFactorRegisterParams;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

import static fr.xephi.authme.process.register.RegisterSecondaryArgument.CONFIRMATION;
import static fr.xephi.authme.process.register.RegisterSecondaryArgument.EMAIL_MANDATORY;
import static fr.xephi.authme.process.register.RegisterSecondaryArgument.EMAIL_OPTIONAL;
import static fr.xephi.authme.process.register.RegisterSecondaryArgument.NONE;
import static fr.xephi.authme.settings.properties.RegistrationSettings.REGISTER_SECOND_ARGUMENT;

/**
 * Command for /register.
 */
public class RegisterCommand extends PlayerCommand {

    @Inject
    private Management management;

    @Inject
    private CommonService commonService;

    @Inject
    private EmailService emailService;

    @Inject
    private ValidationService validationService;

    @Inject
    private RegistrationCaptchaManager registrationCaptchaManager;

    @Override
    public void runCommand(Player player, List<String> arguments) {
        if (!isCaptchaFulfilled(player)) {
            return; // isCaptchaFulfilled handles informing the player on failure
        }

        if (commonService.getProperty(SecuritySettings.PASSWORD_HASH) == HashAlgorithm.TWO_FACTOR) {
            //for two factor auth we don't need to check the usage
            management.performRegister(RegistrationMethod.TWO_FACTOR_REGISTRATION,
                TwoFactorRegisterParams.of(player));
            return;
        } else if (arguments.size() < 1) {
            commonService.send(player, MessageKey.USAGE_REGISTER);
            return;
        }

        RegistrationType registrationType = commonService.getProperty(RegistrationSettings.REGISTRATION_TYPE);
        if (registrationType == RegistrationType.PASSWORD) {
            handlePasswordRegistration(player, arguments);
        } else if (registrationType == RegistrationType.EMAIL) {
            handleEmailRegistration(player, arguments);
        } else {
            throw new IllegalStateException("Unknown registration type '" + registrationType + "'");
        }
    }

    @Override
    protected String getAlternativeCommand() {
        return "/authme register <playername> <password>";
    }

    @Override
    public MessageKey getArgumentsMismatchMessage() {
        return MessageKey.USAGE_REGISTER;
    }

    private boolean isCaptchaFulfilled(Player player) {
        if (registrationCaptchaManager.isCaptchaRequired(player.getName())) {
            String code = registrationCaptchaManager.getCaptchaCodeOrGenerateNew(player.getName());
            commonService.send(player, MessageKey.CAPTCHA_FOR_REGISTRATION_REQUIRED, code);
            return false;
        }
        return true;
    }

    private void handlePasswordRegistration(Player player, List<String> arguments) {
        if (isSecondArgValidForPasswordRegistration(player, arguments)) {
            final String password = arguments.get(0);
            final String email = getEmailIfAvailable(arguments);

            management.performRegister(RegistrationMethod.PASSWORD_REGISTRATION,
                PasswordRegisterParams.of(player, password, email));
        }
    }

    private String getEmailIfAvailable(List<String> arguments) {
        if (arguments.size() >= 2) {
            RegisterSecondaryArgument secondArgType = commonService.getProperty(REGISTER_SECOND_ARGUMENT);
            if (secondArgType == EMAIL_MANDATORY || secondArgType == EMAIL_OPTIONAL) {
                return arguments.get(1);
            }
        }
        return null;
    }

    /**
     * Verifies that the second argument is valid (based on the configuration)
     * to perform a password registration. The player is informed if the check
     * is unsuccessful.
     *
     * @param player the player to register
     * @param arguments the provided arguments
     * @return true if valid, false otherwise
     */
    private boolean isSecondArgValidForPasswordRegistration(Player player, List<String> arguments) {
        RegisterSecondaryArgument secondArgType = commonService.getProperty(REGISTER_SECOND_ARGUMENT);
        // cases where args.size < 2
        if (secondArgType == NONE || secondArgType == EMAIL_OPTIONAL && arguments.size() < 2) {
            return true;
        } else if (arguments.size() < 2) {
            commonService.send(player, MessageKey.USAGE_REGISTER);
            return false;
        }

        if (secondArgType == CONFIRMATION) {
            if (arguments.get(0).equals(arguments.get(1))) {
                return true;
            } else {
                commonService.send(player, MessageKey.PASSWORD_MATCH_ERROR);
                return false;
            }
        } else if (secondArgType == EMAIL_MANDATORY || secondArgType == EMAIL_OPTIONAL) {
            if (validationService.validateEmail(arguments.get(1))) {
                return true;
            } else {
                commonService.send(player, MessageKey.INVALID_EMAIL);
                return false;
            }
        } else {
            throw new IllegalStateException("Unknown secondary argument type '" + secondArgType + "'");
        }
    }

    private void handleEmailRegistration(Player player, List<String> arguments) {
        if (!emailService.hasAllInformation()) {
            commonService.send(player, MessageKey.INCOMPLETE_EMAIL_SETTINGS);
            ConsoleLogger.warning("Cannot register player '" + player.getName() + "': no email or password is set "
                + "to send emails from. Please adjust your config at " + EmailSettings.MAIL_ACCOUNT.getPath());
            return;
        }

        final String email = arguments.get(0);
        if (!validationService.validateEmail(email)) {
            commonService.send(player, MessageKey.INVALID_EMAIL);
        } else if (isSecondArgValidForEmailRegistration(player, arguments)) {
            management.performRegister(RegistrationMethod.EMAIL_REGISTRATION,
                EmailRegisterParams.of(player, email));
        }
    }

    /**
     * Verifies that the second argument is valid (based on the configuration)
     * to perform an email registration. The player is informed if the check
     * is unsuccessful.
     *
     * @param player the player to register
     * @param arguments the provided arguments
     * @return true if valid, false otherwise
     */
    private boolean isSecondArgValidForEmailRegistration(Player player, List<String> arguments) {
        RegisterSecondaryArgument secondArgType = commonService.getProperty(REGISTER_SECOND_ARGUMENT);
        // cases where args.size < 2
        if (secondArgType == NONE || secondArgType == EMAIL_OPTIONAL && arguments.size() < 2) {
            return true;
        } else if (arguments.size() < 2) {
            commonService.send(player, MessageKey.USAGE_REGISTER);
            return false;
        }

        if (secondArgType == EMAIL_OPTIONAL || secondArgType == EMAIL_MANDATORY || secondArgType == CONFIRMATION) {
            if (arguments.get(0).equals(arguments.get(1))) {
                return true;
            } else {
                commonService.send(player, MessageKey.USAGE_REGISTER);
                return false;
            }
        } else {
            throw new IllegalStateException("Unknown secondary argument type '" + secondArgType + "'");
        }
    }
}
