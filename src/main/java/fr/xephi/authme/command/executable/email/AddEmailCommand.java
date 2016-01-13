package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.entity.Player;

import java.util.List;

public class AddEmailCommand extends PlayerCommand {

    @Override
    public void runCommand(Player player, List<String> arguments, CommandService commandService) {
        String email = arguments.get(0);
        String emailConfirmation = arguments.get(1);

        if (StringUtils.isEmpty(email) || "your@email.com".equals(email) || !Settings.isEmailCorrect(email)) {
            commandService.send(player, MessageKey.INVALID_EMAIL);
        } else if (email.equals(emailConfirmation)) {
            commandService.getManagement().performAddEmail(player, email);
        } else {
            commandService.send(player, MessageKey.CONFIRM_EMAIL_MESSAGE);
        }
    }
}
