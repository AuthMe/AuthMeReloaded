package fr.xephi.authme.command.executable.authme.debug;

import fr.xephi.authme.listener.FailedVerificationException;
import fr.xephi.authme.listener.OnJoinVerifier;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.permission.DebugSectionPermissions;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.service.ValidationService.ValidationResult;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

import static fr.xephi.authme.command.executable.authme.debug.InputValidator.ValidationObject.MAIL;
import static fr.xephi.authme.command.executable.authme.debug.InputValidator.ValidationObject.NAME;
import static fr.xephi.authme.command.executable.authme.debug.InputValidator.ValidationObject.PASS;

/**
 * Checks if a sample username, email or password is valid according to the AuthMe settings.
 */
class InputValidator implements DebugSection {

    @Inject
    private ValidationService validationService;

    @Inject
    private Messages messages;

    @Inject
    private OnJoinVerifier onJoinVerifier;


    @Override
    public String getName() {
        return "valid";
    }

    @Override
    public String getDescription() {
        return "Checks if your config.yml allows a password / email";
    }

    @Override
    public void execute(CommandSender sender, List<String> arguments) {
        if (arguments.size() < 2 || !ValidationObject.matchesAny(arguments.get(0))) {
            displayUsageHint(sender);

        } else if (PASS.matches(arguments.get(0))) {
            validatePassword(sender, arguments.get(1));

        } else if (MAIL.matches(arguments.get(0))) {
            validateEmail(sender, arguments.get(1));

        } else if (NAME.matches(arguments.get(0))) {
            validateUsername(sender, arguments.get(1));

        } else {
            throw new IllegalStateException("Unexpected validation object with arg[0] = '" + arguments.get(0) + "'");
        }
    }

    @Override
    public PermissionNode getRequiredPermission() {
        return DebugSectionPermissions.INPUT_VALIDATOR;
    }

    private void displayUsageHint(CommandSender sender) {
        sender.sendMessage(ChatColor.BLUE + "Validation tests");
        sender.sendMessage("You can define forbidden emails and passwords in your config.yml."
            + " You can test your settings with this command.");
        final String command = ChatColor.GOLD + "/authme debug valid";
        sender.sendMessage(" Use " + command + " pass <pass>" + ChatColor.RESET + " to check a password");
        sender.sendMessage(" Use " + command + " mail <mail>" + ChatColor.RESET + " to check an email");
        sender.sendMessage(" Use " + command + " name <name>" + ChatColor.RESET + " to check a username");
    }

    private void validatePassword(CommandSender sender, String password) {
        ValidationResult validationResult = validationService.validatePassword(password, "");
        sender.sendMessage(ChatColor.BLUE + "Validation of password '" + password + "'");
        if (validationResult.hasError()) {
            messages.send(sender, validationResult.getMessageKey(), validationResult.getArgs());
        } else {
            sender.sendMessage(ChatColor.DARK_GREEN + "Valid password!");
        }
    }

    private void validateEmail(CommandSender sender, String email) {
        boolean isValidEmail = validationService.validateEmail(email);
        sender.sendMessage(ChatColor.BLUE + "Validation of email '" + email + "'");
        if (isValidEmail) {
            sender.sendMessage(ChatColor.DARK_GREEN + "Valid email!");
        } else {
            sender.sendMessage(ChatColor.DARK_RED + "Email is not valid!");
        }
    }

    private void validateUsername(CommandSender sender, String username) {
        sender.sendMessage(ChatColor.BLUE + "Validation of username '" + username + "'");
        try {
            onJoinVerifier.checkIsValidName(username);
            sender.sendMessage("Valid username!");
        } catch (FailedVerificationException failedVerificationEx) {
            messages.send(sender, failedVerificationEx.getReason(), failedVerificationEx.getArgs());
        }
    }


    enum ValidationObject {

        PASS, MAIL, NAME;

        static boolean matchesAny(String arg) {
            return Arrays.stream(values()).anyMatch(vo -> vo.matches(arg));
        }

        boolean matches(String arg) {
            return name().equalsIgnoreCase(arg);
        }
    }

}
