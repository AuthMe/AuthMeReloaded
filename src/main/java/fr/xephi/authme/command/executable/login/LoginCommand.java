package fr.xephi.authme.command.executable.login;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.util.Wrapper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class LoginCommand extends ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        // Make sure the current command executor is a player
        if (!(sender instanceof Player)) {
            return;
        }

        // Get the necessary objects
        final AuthMe plugin = Wrapper.getInstance().getAuthMe();
        final Player player = (Player) sender;
        final String password = arguments.get(0);

        // Log the player in
        plugin.getManagement().performLogin(player, password, false);
    }
}
