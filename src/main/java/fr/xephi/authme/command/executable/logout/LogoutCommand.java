package fr.xephi.authme.command.executable.logout;

import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.process.Management;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * Logout command.
 */
public class LogoutCommand extends PlayerCommand {

    @Inject
    private Management management;

    @Override
    public void runCommand(Player player, List<String> arguments) {
        management.performLogout(player);
    }
}
