package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.ValidationService;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * Command for setting an email to an account.
 */
public class AddEmailCommand extends PlayerCommand {

    @Inject
    private Management management;

    @Inject
    private CommonService commonService;

    @Inject
    private ValidationService validationService;

    @Override
    public void runCommand(Player player, List<String> arguments) {
        String email = arguments.get(0);
        String emailConfirmation = arguments.get(1);

        if (!validationService.validateEmail(email)) {
            commonService.send(player, MessageKey.INVALID_EMAIL);
        } else if (email.equals(emailConfirmation)) {
            management.performAddEmail(player, email);
        } else {
            commonService.send(player, MessageKey.CONFIRM_EMAIL_MESSAGE);
        }
    }

    @Override
    public MessageKey getArgumentsMismatchMessage() {
        return MessageKey.USAGE_ADD_EMAIL;
    }
}
