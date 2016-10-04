package fr.xephi.authme.command.executable.changepassword;

import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.service.ValidationService.ValidationResult;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * The command for a player to change his password with.
 */
public class ChangePasswordCommand extends PlayerCommand {

    @Inject
    private CommandService commandService;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private ValidationService validationService;

    @Inject
    private Management management;

    @Override
    public void runCommand(Player player, List<String> arguments) {
        String oldPassword = arguments.get(0);
        String newPassword = arguments.get(1);

        String name = player.getName().toLowerCase();
        if (!playerCache.isAuthenticated(name)) {
            commandService.send(player, MessageKey.NOT_LOGGED_IN);
            return;
        }

        // Make sure the password is allowed
        ValidationResult passwordValidation = validationService.validatePassword(newPassword, name);
        if (passwordValidation.hasError()) {
            commandService.send(player, passwordValidation.getMessageKey(), passwordValidation.getArgs());
            return;
        }

        management.performPasswordChange(player, oldPassword, newPassword);
    }
}
