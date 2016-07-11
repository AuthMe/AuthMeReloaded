package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.permission.AuthGroupHandler;
import fr.xephi.authme.permission.AuthGroupType;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.util.BukkitService;
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
    private CommandService commandService;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private AuthGroupHandler authGroupHandler;

    @Inject
    private Management management;


    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments) {
        // Get the player name
        String playerName = arguments.get(0);
        String playerNameLowerCase = playerName.toLowerCase();

        // Make sure the user is valid
        if (!dataSource.isAuthAvailable(playerNameLowerCase)) {
            commandService.send(sender, MessageKey.UNKNOWN_USER);
            return;
        }

        // Remove the player
        if (!dataSource.removeAuth(playerNameLowerCase)) {
            commandService.send(sender, MessageKey.ERROR);
            return;
        }

        // Unregister the player
        Player target = bukkitService.getPlayerExact(playerNameLowerCase);
        playerCache.removePlayer(playerNameLowerCase);
        authGroupHandler.setGroup(target, AuthGroupType.UNREGISTERED);
        if (target != null && target.isOnline()) {
            management.performUnregister(target, "dontneed", true);
        }

        // Show a status message
        commandService.send(sender, MessageKey.UNREGISTERED_SUCCESS);
        ConsoleLogger.info(sender.getName() + " unregistered " + playerName);
    }
}
