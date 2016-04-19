package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.util.Utils;
import org.bukkit.entity.Player;

import java.util.List;

public class AddEmailCommand extends PlayerCommand {

    @Override
    public void runCommand(Player player, List<String> arguments, CommandService commandService) {
        String email = arguments.get(0);
        String emailConfirmation = arguments.get(1);

        if (!Utils.isEmailCorrect(email, commandService.getSettings())) {
            commandService.send(player, MessageKey.INVALID_EMAIL);
        } else if (email.equals(emailConfirmation)) {
            commandService.getManagement().performAddEmail(player, email);
        } else {
            commandService.send(player, MessageKey.CONFIRM_EMAIL_MESSAGE);
        }
    }
}
