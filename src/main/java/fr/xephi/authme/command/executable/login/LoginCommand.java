package fr.xephi.authme.command.executable.login;

import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.Management;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * Login command.
 */
public class LoginCommand extends PlayerCommand {

    @Inject
    private Management management;

    @Override
    public void runCommand(Player player, List<String> arguments) {
        String password = arguments.get(0);
        management.performLogin(player, password);
    }

    @Override
    public MessageKey getArgumentsMismatchMessage() {
        return MessageKey.USAGE_LOGIN;
    }

    @Override
    protected String getAlternativeCommand() {
        return "/authme forcelogin <player>";
    }
}
