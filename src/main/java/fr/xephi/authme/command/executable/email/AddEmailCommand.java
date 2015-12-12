package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.util.Wrapper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class AddEmailCommand extends ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        // Make sure the current command executor is a player
        if (!(sender instanceof Player)) {
            return;
        }

        // Get the parameter values
        String playerMail = arguments.get(0);
        String playerMailVerify = arguments.get(1);

        // Get the player and perform email addition
        final AuthMe plugin = Wrapper.getInstance().getAuthMe();
        final Player player = (Player) sender;
        plugin.getManagement().performAddEmail(player, playerMail, playerMailVerify);
    }
}
