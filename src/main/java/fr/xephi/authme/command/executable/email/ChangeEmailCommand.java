package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.data.VerificationCodeManager;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.CommonService;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * Change email command.
 */
public class ChangeEmailCommand extends PlayerCommand {

    @Inject
    private Management management;

    @Inject
    private CommonService commonService;

    @Inject
    private VerificationCodeManager codeManager;

    @Override
    public void runCommand(Player player, List<String> arguments) {
        final String playerName = player.getName();
        // Check if the user has been verified or not
        if (codeManager.isVerificationRequired(player)) {
            codeManager.codeExistOrGenerateNew(playerName);
            commonService.send(player, MessageKey.VERIFICATION_CODE_REQUIRED);
            return;
        }

        String playerMailOld = arguments.get(0);
        String playerMailNew = arguments.get(1);
        management.performChangeEmail(player, playerMailOld, playerMailNew);
    }

    @Override
    public MessageKey getArgumentsMismatchMessage() {
        return MessageKey.USAGE_CHANGE_EMAIL;
    }
}
