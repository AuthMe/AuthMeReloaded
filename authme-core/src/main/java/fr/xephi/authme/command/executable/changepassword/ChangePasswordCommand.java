package fr.xephi.authme.command.executable.changepassword;

import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.data.VerificationCodeManager;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.service.ValidationService.ValidationResult;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;

/**
 * The command for a player to change his password with.
 */
public class ChangePasswordCommand extends PlayerCommand {

    @Inject
    private CommonService commonService;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private ValidationService validationService;

    @Inject
    private Management management;

    @Inject
    private VerificationCodeManager codeManager;

    @Override
    public void runCommand(Player player, List<String> arguments) {
        String name = player.getName().toLowerCase(Locale.ROOT);

        if (!playerCache.isAuthenticated(name)) {
            commonService.send(player, MessageKey.NOT_LOGGED_IN);
            return;
        }

        // Check if the user has been verified or not
        if (codeManager.isVerificationRequired(player)) {
            codeManager.codeExistOrGenerateNew(name);
            commonService.send(player, MessageKey.VERIFICATION_CODE_REQUIRED);
            return;
        }

        String oldPassword = arguments.get(0);
        String newPassword = arguments.get(1);

        // Make sure the password is allowed
        ValidationResult passwordValidation = validationService.validatePassword(newPassword, name);
        if (passwordValidation.hasError()) {
            commonService.send(player, passwordValidation.getMessageKey(), passwordValidation.getArgs());
            return;
        }

        management.performPasswordChange(player, oldPassword, newPassword);
    }

    @Override
    protected String getAlternativeCommand() {
        return "/authme password <playername> <password>";
    }

    @Override
    public MessageKey getArgumentsMismatchMessage() {
        return MessageKey.USAGE_CHANGE_PASSWORD;
    }
}
