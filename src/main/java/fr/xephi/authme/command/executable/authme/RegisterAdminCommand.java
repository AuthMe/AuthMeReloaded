package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.security.crypts.HashedPassword;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Admin command to register a user.
 */
public class RegisterAdminCommand implements ExecutableCommand {

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments,
                               final CommandService commandService) {
        // Get the player name and password
        final String playerName = arguments.get(0);
        final String playerPass = arguments.get(1);
        final String playerNameLowerCase = playerName.toLowerCase();

        // Command logic
        MessageKey passwordError = commandService.validatePassword(playerPass, playerName);
        if (passwordError != null) {
            commandService.send(sender, passwordError);
            return;
        }

        commandService.runTaskAsynchronously(new Runnable() {

            @Override
            public void run() {
                if (commandService.getDataSource().isAuthAvailable(playerNameLowerCase)) {
                    commandService.send(sender, MessageKey.NAME_ALREADY_REGISTERED);
                    return;
                }
                HashedPassword hashedPassword = commandService.getPasswordSecurity()
                    .computeHash(playerPass, playerNameLowerCase);
                PlayerAuth auth = PlayerAuth.builder()
                    .name(playerNameLowerCase)
                    .realName(playerName)
                    .password(hashedPassword)
                    .build();

                if (!commandService.getDataSource().saveAuth(auth)) {
                    commandService.send(sender, MessageKey.ERROR);
                    return;
                }
                commandService.getDataSource().setUnlogged(playerNameLowerCase);

                commandService.send(sender, MessageKey.REGISTER_SUCCESS);
                ConsoleLogger.info(sender.getName() + " registered " + playerName);
                Player player = commandService.getPlayer(playerName);
                if (player != null) {
                    player.kickPlayer("An admin just registered you, please log in again");
                }
            }
        });
    }
}
