package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.Management;
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
    private CommandService commandService;

    @Override
    public void runCommand(Player player, List<String> arguments) {
        String email = arguments.get(0);
        String emailConfirmation = arguments.get(1);

        if (email.equals(emailConfirmation)) {
            // Closer inspection of the mail address handled by the async task
            management.performAddEmail(player, email);
        } else {
            commandService.send(player, MessageKey.CONFIRM_EMAIL_MESSAGE);
        }
    }
}
