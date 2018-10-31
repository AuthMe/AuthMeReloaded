package fr.xephi.authme.command.executable.email;

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

    @Override
    public void runCommand(Player player, List<String> arguments) {
        String email = arguments.get(0);
        management.performAddEmail(player, email);
    }

    @Override
    public MessageKey getArgumentsMismatchMessage() {
        return MessageKey.USAGE_ADD_EMAIL;
    }
}
