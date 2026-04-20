package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * Admin command to unregister a player.
 */
public class UnregisterAdminCommand implements ExecutableCommand {

    @Inject
    private DataSource dataSource;

    @Inject
    private CommonService commonService;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private Management management;

    UnregisterAdminCommand() {
    }

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments) {
        String playerName = arguments.get(0);

        // Make sure the user exists
        if (!dataSource.isAuthAvailable(playerName)) {
            commonService.send(sender, MessageKey.UNKNOWN_USER);
            return;
        }

        // Get the player from the server and perform unregister
        Player target = bukkitService.getPlayerExact(playerName);
        management.performUnregisterByAdmin(sender, playerName, target);
    }
}
