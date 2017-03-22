package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.PasswordRecoveryService;
import fr.xephi.authme.service.RecoveryCodeService;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * Command for submitting email recovery code.
 */
public class ProcessCodeCommand extends PlayerCommand {

    @Inject
    private CommonService commonService;

    @Inject
    private RecoveryCodeService codeService;

    @Inject
    private PasswordRecoveryService recoveryService;

    @Override
    protected void runCommand(Player player, List<String> arguments) {
        String name = player.getName();
        String code = arguments.get(0);

        if (codeService.hasTriesLeft(name)) {
            if (codeService.isCodeValid(name, code)) {
                commonService.send(player, MessageKey.RECOVERY_CODE_CORRECT);
                recoveryService.addSuccessfulRecovery(player);
                codeService.removeCode(name);
            } else {
                commonService.send(player, MessageKey.INCORRECT_RECOVERY_CODE,
                    Integer.toString(codeService.getTriesLeft(name)));
            }
        } else {
            codeService.removeCode(name);
            commonService.send(player, MessageKey.RECOVERY_TRIES_EXCEEDED);
        }
    }
}
